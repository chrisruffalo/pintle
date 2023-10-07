package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.github.chrisruffalo.pintle.resolution.responder.Responder;
import io.github.chrisruffalo.pintle.resolution.responder.TcpResponder;
import io.github.chrisruffalo.pintle.resolution.responder.UdpResponder;
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
import org.jboss.logging.Logger;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Opcode;
import org.xbill.DNS.Section;

import java.io.IOException;

@ApplicationScoped
public class ServerController {

    private static final String SERVER_HOST = "0.0.0.0";

    private static final int SERVER_PORT = 5353;

    private static final String MDNS_LISTEN_ADDRESS = "224.0.0.251";

    private static final int MDNS_PORT = 5353;

    @Inject
    Logger logger;

    @Inject
    Vertx vertx;

    @Inject
    EventBus eventBus;

    @Inject
    Tracer tracer;

    NetServer tcpServer;
    DatagramSocket udpServer;
    DatagramSocket mdnsServer;


    private void startTcpServer() {
        final NetServerOptions options = new NetServerOptions()
                .setPort(SERVER_PORT)
                .setSsl(false).setHost(SERVER_HOST)
                .setReuseAddress(true);
        tcpServer = vertx.createNetServer(options);


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
                    // send event, wait for result
                    eventBus.send(Bus.CHECK_CACHE, new QueryContext(traceId, span, responder, message));
                } catch (Exception ex) {
                    // send error to be handled
                    eventBus.send(Bus.HANDLE_ERROR, new QueryContext(traceId, span, responder, ex));
                }
            });

            socket.closeHandler(event -> logger.debugf("[TCP] connection closed %s:%s", socket.remoteAddress().host(), socket.remoteAddress().port()));
        });

        tcpServer.listen(asyncResult -> {
            if (asyncResult.succeeded()) {
                logger.infof("[TCP] Server is listening on %s:%s", SERVER_HOST, SERVER_PORT);
            } else {
                logger.errorf("[TCP] Server listen failed on %s:%s - %s", SERVER_HOST, SERVER_PORT, asyncResult.cause());
            }
        });
    }

    private void startUdpServer() {
        udpServer = vertx.createDatagramSocket(new DatagramSocketOptions().setIpV6(false).setReuseAddress(true));
        udpServer.listen(SERVER_PORT, SERVER_HOST, asyncResult -> {
            if (asyncResult.succeeded()) {
                logger.infof("[UDP] Server is listening on %s:%d", SERVER_HOST, SERVER_PORT);

                udpServer.handler(packet -> {
                    final Span span = tracer.spanBuilder("udp").startSpan();
                    final String traceId = span.getSpanContext().getTraceId();

                    byte[] questionBytes = packet.data().getBytes();
                    logger.debugf("[UDP] message received from %s:%s, length: %d", packet.sender().host(), packet.sender().port(), questionBytes.length);
                    final Responder responder = new UdpResponder(udpServer, packet.sender().host(), packet.sender().port());
                    try {
                        final Message message = new Message(questionBytes);
                        // send event, wait for result
                        eventBus.send(Bus.CHECK_CACHE, new QueryContext(traceId, span, responder, message));
                    } catch (Exception ex) {
                        // send error to be handled
                        eventBus.send(Bus.HANDLE_ERROR, new QueryContext(traceId, span, responder, ex));
                    }
                });
            } else {
                logger.errorf("[UDP] Server listen failed on %s:%d - %s", SERVER_HOST, SERVER_PORT, asyncResult.cause());
            }
        });
    }

    private void startMdnsUdpServer() {
        mdnsServer = vertx.createDatagramSocket(new DatagramSocketOptions().setMulticastNetworkInterface("enp6s18").setIpV6(false).setReuseAddress(true));
        mdnsServer.listen(MDNS_PORT, MDNS_LISTEN_ADDRESS, asyncResult -> {
            if (asyncResult.succeeded()) {
                logger.infof("[MDNS] Server is listening on %s:%d", MDNS_LISTEN_ADDRESS, MDNS_PORT);

                mdnsServer.listenMulticastGroup(MDNS_LISTEN_ADDRESS, handler -> {
                    mdnsServer.handler(packet -> {

                        byte[] questionBytes = packet.data().getBytes();
                        try {
                            final Message message = new Message(questionBytes);
                            if (message.getHeader().getFlag(Flags.QR)) {
                                final Span span = tracer.spanBuilder("mdns-store").startSpan();
                                final String traceId = span.getSpanContext().getTraceId();
                                final QueryContext context = new QueryContext(traceId, null, message);
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
    }

    public void startServers(@Observes StartupEvent startupEvent) {
        startTcpServer();
        startUdpServer();
        startMdnsUdpServer();
    }

    public void stopServers(@Observes ShutdownEvent shutdownEvent) {
        if(tcpServer !=null) {
            tcpServer.close().onComplete(handler -> {
                tcpServer = null;
                logger.infof("[TCP] shutdown");
            });
        }
        if(udpServer != null) {
            udpServer.close().onComplete(handler -> {
                udpServer = null;
                logger.infof("[UDP] shutdown");
            });
        }
        if(mdnsServer != null) {
            mdnsServer.close().onComplete(handler -> {
                mdnsServer = null;
                logger.infof("[MDNS] shutdown");
            });
        }
    }

}
