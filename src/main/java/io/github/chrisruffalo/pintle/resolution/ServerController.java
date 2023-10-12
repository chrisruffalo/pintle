package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.config.Listener;
import io.github.chrisruffalo.pintle.config.Mdns;
import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.Resolver;
import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.github.chrisruffalo.pintle.model.ServiceType;
import io.github.chrisruffalo.pintle.resolution.responder.Responder;
import io.github.chrisruffalo.pintle.resolution.responder.TcpResponder;
import io.github.chrisruffalo.pintle.resolution.responder.UdpResponder;
import io.github.chrisruffalo.pintle.resolution.server.ListenerHolder;
import io.github.chrisruffalo.pintle.resolution.server.MdnsListenerHolder;
import io.github.chrisruffalo.pintle.resolution.server.TcpListenerHolder;
import io.github.chrisruffalo.pintle.resolution.server.UdpListenerHolder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Link;
import org.jboss.logging.Logger;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Opcode;
import org.xbill.DNS.Section;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

@ApplicationScoped
public class ServerController {

    private static final String SERVER_HOST = "0.0.0.0";

    private static final int SERVER_PORT = 5353;

    private static final String MDNS_LISTEN_ADDRESS = "224.0.0.251";

    private static final int MDNS_PORT = 5353;

    @Inject
    PintleConfig config;

    @Inject
    Logger logger;

    @Inject
    Vertx vertx;

    @Inject
    EventBus eventBus;

    @Inject
    Tracer tracer;

    private final List<ListenerHolder> servers = new LinkedList<>();

    private ListenerHolder startServer(final Listener config) {
        if(ServiceType.TCP.equals(config.type())) {
            return startTcpServer(config);
        } else if (ServiceType.UDP.equals(config.type())) {
            return startUdpServer(config);
        }
        return null;
    }

    private ListenerHolder startTcpServer(final Listener config) {
        final int port = config.port().orElse(SERVER_PORT);

        final NetServerOptions options = new NetServerOptions()
                .setPort(port)
                .setSsl(false).setHost(SERVER_HOST)
                .setReuseAddress(true);
        final NetServer tcpServer = vertx.createNetServer(options);


        tcpServer.connectHandler(socket -> {
            logger.debugf("[TCP] connection from %s:%s", socket.remoteAddress().host(), socket.remoteAddress().port());

            socket.handler(buffer -> {
                final Span span = tracer.spanBuilder("tcp").startSpan();
                final String traceId = span.getSpanContext().getTraceId();
                final Responder responder = new TcpResponder(socket, socket.remoteAddress().host(), socket.remoteAddress().port());

                if (buffer.length() <= 2) {
                    eventBus.send(Bus.HANDLE_ERROR, new QueryContext(traceId, responder, new IllegalStateException("a dns message cannot be less than 2 bytes")));
                    return;
                }

                final byte[] lengthBytes = buffer.getBytes(0,2);
                final int expectedLength = ((lengthBytes[0] & 0xff) << 8) | (lengthBytes[1] & 0xff);
                final byte[] questionBytes = buffer.getBytes(2, buffer.length());

                logger.debugf("[TCP] message received from %s:%s, length: %d (expected: %d)", socket.remoteAddress().host(), socket.remoteAddress().port(), questionBytes.length, expectedLength);

                try {
                    final Message message = new Message(questionBytes);
                    final QueryContext context = new QueryContext(traceId, span, responder, message);
                    context.setListenerName(config.name());
                    // send event, wait for result
                    eventBus.send(Bus.ASSIGN_GROUP, context);
                } catch (Exception ex) {
                    final QueryContext context = new QueryContext(traceId, span, responder, ex);
                    context.setListenerName(config.name());
                    // send error to be handled
                    eventBus.send(Bus.HANDLE_ERROR, context);
                }
            });

            socket.closeHandler(event -> logger.debugf("[TCP] connection closed %s:%s", socket.remoteAddress().host(), socket.remoteAddress().port()));
        });

        tcpServer.listen(asyncResult -> {
            if (asyncResult.succeeded()) {
                logger.infof("[TCP] Server is listening on %s:%d", SERVER_HOST, port);
            } else {
                logger.errorf("[TCP] Server listen failed on %s:%d - %s", SERVER_HOST, port, asyncResult.cause());
            }
        });

        return new TcpListenerHolder(config, tcpServer);
    }

    private ListenerHolder startUdpServer(final Listener config) {
        final DatagramSocket udpServer = vertx.createDatagramSocket(new DatagramSocketOptions().setIpV6(false).setReuseAddress(true));
        int port = config.port().orElse(SERVER_PORT);
        udpServer.listen(port, SERVER_HOST, asyncResult -> {
            if (asyncResult.succeeded()) {
                logger.infof("[UDP] Server is listening on %s:%d", SERVER_HOST, port);

                udpServer.handler(packet -> {
                    final Span span = tracer.spanBuilder("udp").startSpan();
                    final String traceId = span.getSpanContext().getTraceId();

                    byte[] questionBytes = packet.data().getBytes();
                    logger.debugf("[UDP] message received from %s:%s, length: %d", packet.sender().host(), packet.sender().port(), questionBytes.length);
                    final Responder responder = new UdpResponder(udpServer, packet.sender().host(), packet.sender().port());
                    try {
                        final Message message = new Message(questionBytes);
                        final QueryContext context = new QueryContext(traceId, span, responder, message);
                        context.setListenerName(config.name());
                        // send event, wait for result
                        eventBus.send(Bus.ASSIGN_GROUP, context);
                    } catch (Exception ex) {
                        final QueryContext context = new QueryContext(traceId, span, responder, ex);
                        context.setListenerName(config.name());
                        // send error to be handled
                        eventBus.send(Bus.HANDLE_ERROR, context);
                    }
                });
            } else {
                logger.errorf("[UDP] Server listen failed on %s:%d - %s", SERVER_HOST, SERVER_PORT, asyncResult.cause());
            }
        });

        return new UdpListenerHolder(config, udpServer);
    }

    private ListenerHolder startMdnsUdpServer(final String mdnsInterface) {
        final DatagramSocket mdnsServer = vertx.createDatagramSocket(new DatagramSocketOptions().setMulticastNetworkInterface(mdnsInterface).setIpV6(false).setReuseAddress(true));
        mdnsServer.listen(MDNS_PORT, MDNS_LISTEN_ADDRESS, asyncResult -> {
            if (asyncResult.succeeded()) {
                logger.infof("[MDNS] on interface=%s listening on %s:%d", mdnsInterface, MDNS_LISTEN_ADDRESS, MDNS_PORT);

                mdnsServer.listenMulticastGroup(MDNS_LISTEN_ADDRESS, handler -> {
                    mdnsServer.handler(packet -> {

                        byte[] questionBytes = packet.data().getBytes();
                        try {
                            final Message message = new Message(questionBytes);
                            if (message.getHeader().getFlag(Flags.QR)) {
                                final Span span = tracer.spanBuilder("mdns-store").startSpan();
                                final String traceId = span.getSpanContext().getTraceId();
                                final QueryContext context = new QueryContext(traceId, null, message);
                                context.setListenerName("mdns-" + mdnsInterface);
                                eventBus.send(Bus.STORE_MDNS, context);
                            } else {
                                logger.debugf("[MDNS] multicast message received query: %s", message.getQuestion().getName().toString(true));
                            }
                        } catch (IOException e) {
                            // nothing, we don't care about non-dns traffic on this address at all
                        }
                    });
                });
            } else {
                logger.errorf("[MDNS] Server listen failed on %s:%d - %s", MDNS_LISTEN_ADDRESS, MDNS_PORT, asyncResult.cause());
            }
        });
        return new MdnsListenerHolder("mdns-" + mdnsInterface, mdnsServer);
    }

    public void startServers(@Observes StartupEvent startupEvent) {
        servers.clear();
        if (config.listeners().isEmpty()) {
            return;
        }
        List<Listener> listeners = config.listeners().get();
        for (final Listener listener : listeners) {
            servers.add(startServer(listener));
        }

        final Mdns mdns = config.mdns();
        if (mdns.enabled()) {
            if(mdns.interfaces().stream().anyMatch("all"::equalsIgnoreCase)) {
                try {
                    Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
                    for(final NetworkInterface ne : Collections.list(interfaceEnumeration)) {
                        servers.add(startMdnsUdpServer(ne.getName()));
                    }
                } catch (SocketException e) {
                    logger.errorf("could not enumerate 'all' interfaces for mdns: %s", e.getMessage());
                }
            } else {
                for (String i : mdns.interfaces()) {
                    servers.add(startMdnsUdpServer(i));
                }
            }
        }
    }

    public void stopServers(@Observes ShutdownEvent shutdownEvent) {
        for (final ListenerHolder server : this.servers) {
            // services with unspecified types go here
            if (server == null) {
                continue;
            }

            server.stop().onComplete(handler -> {
              if (handler.succeeded()) {
                  logger.infof("shutdown server %s", server.name());
              } else if(handler.cause() != null) {
                  logger.infof("failed to shutdown server %s: %s", server.name(), handler.cause().getMessage());
              } else {
                  logger.infof("failed to shutdown server %s: %s", server.name(), handler.cause().getMessage());
              }
            }).result();
        }
    }

}
