package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.config.Listener;
import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.event.ConfigUpdate;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.github.chrisruffalo.pintle.model.ServiceType;
import io.github.chrisruffalo.pintle.resolution.responder.Responder;
import io.github.chrisruffalo.pintle.resolution.responder.TcpResponder;
import io.github.chrisruffalo.pintle.resolution.responder.UdpResponder;
import io.github.chrisruffalo.pintle.resolution.server.ListenerHolder;
import io.github.chrisruffalo.pintle.resolution.server.TcpListenerHolder;
import io.github.chrisruffalo.pintle.resolution.server.UdpListenerHolder;
import io.opentelemetry.api.trace.Span;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.xbill.DNS.Message;

import java.util.List;

@ApplicationScoped
public class ListenerController extends AbstractListenerController {

    private static final String SERVER_HOST = "0.0.0.0";

    private static final int SERVER_PORT = 5353;

    @Inject
    Logger logger;

    @Override
    protected Logger logger() {
        return logger;
    }

    private ListenerHolder startServer(final String configId, final Listener config) {
        if(ServiceType.TCP.equals(config.type())) {
            return startTcpServer(configId, config);
        } else if (ServiceType.UDP.equals(config.type())) {
            return startUdpServer(configId, config);
        }
        return null;
    }

    private ListenerHolder startTcpServer(final String configId, final Listener config) {
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
                    context.setConfigId(configId);
                    // send event, wait for result
                    eventBus.send(Bus.ASSIGN_GROUP, context);
                } catch (Exception ex) {
                    final QueryContext context = new QueryContext(traceId, span, responder, ex);
                    context.setListenerName(config.name());
                    context.setConfigId(configId);
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

    private ListenerHolder startUdpServer(final String configId, final Listener config) {
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
                        context.setConfigId(configId);
                        context.setListenerName(config.name());
                        // send event, wait for result
                        eventBus.send(Bus.ASSIGN_GROUP, context);
                    } catch (Exception ex) {
                        final QueryContext context = new QueryContext(traceId, span, responder, ex);
                        context.setConfigId(configId);
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

    @ConsumeEvent(value = Bus.CONFIG_UPDATE_LISTENERS, ordered = true)
    public void configure(ConfigUpdate event) {
        config = configProducer.get(event.getId());
        if (!listeners.isEmpty()) {
            stopServers(null);
        }
        listeners.clear();
        if (config.listeners().isEmpty()) {
            return;
        }
        List<Listener> listeners = config.listeners().get();
        for (final Listener listener : listeners) {
            this.listeners.add(startServer(event.getId(), listener));
        }
    }
}
