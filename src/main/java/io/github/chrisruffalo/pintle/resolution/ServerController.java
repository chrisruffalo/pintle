package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.resolution.dto.QueryContext;
import io.github.chrisruffalo.pintle.resolution.dto.Responder;
import io.github.chrisruffalo.pintle.resolution.dto.TcpResponder;
import io.github.chrisruffalo.pintle.resolution.dto.UdpResponder;
import io.netty.handler.logging.ByteBufFormat;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.xbill.DNS.*;

@ApplicationScoped
public class ServerController {

    private static final String SERVER_HOST = "0.0.0.0";

    private static final int SERVER_PORT = 5353;

    @Inject
    Logger logger;

    @Inject
    Vertx vertx;

    @Inject
    EventBus eventBus;

    public void startServers(@Observes StartupEvent startupEvent) {
        final NetServerOptions options = new NetServerOptions()
                                            .setPort(SERVER_PORT)
                                            .setSsl(false).setHost(SERVER_HOST)
                                            .setReuseAddress(true);
        final NetServer tcpServer = vertx.createNetServer(options);

        tcpServer.connectHandler(socket -> {
            logger.debugf("[TCP] connection from %s:%s", socket.remoteAddress().host(), socket.remoteAddress().port());

            socket.handler(buffer -> {
                final Responder responder = new TcpResponder(socket, socket.remoteAddress().host(), socket.remoteAddress().port());

                if (buffer.length() <= 2) {
                    eventBus.send(Bus.HANDLE_ERROR, new QueryContext(responder, new IllegalStateException("a dns message cannot be less than 2 bytes")));
                    return;
                }

                final byte[] lengthBytes = buffer.getBytes(0,2);
                final int expectedLength = ((lengthBytes[0] & 0xff) << 8) | (lengthBytes[1] & 0xff);
                final byte[] questionBytes = buffer.getBytes(2, buffer.length());

                logger.debugf("[TCP] message received from %s:%s, length: %d (expected: %d)", socket.remoteAddress().host(), socket.remoteAddress().port(), questionBytes.length, expectedLength);

                try {
                    final Message message = new Message(questionBytes);
                    // send event, wait for result
                    eventBus.send(Bus.CHECK_CACHE, new QueryContext(responder, message));
                } catch (Exception ex) {
                    // send error to be handled
                    eventBus.send(Bus.HANDLE_ERROR, new QueryContext(responder, ex));
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


        DatagramSocket udpSocket = vertx.createDatagramSocket(new DatagramSocketOptions().setIpV6(false).setReuseAddress(true));
        udpSocket.listen(SERVER_PORT, SERVER_HOST, asyncResult -> {
            if (asyncResult.succeeded()) {
                logger.infof("[UDP] Server is listening on %s:%d", SERVER_HOST, SERVER_PORT);

                udpSocket.handler(packet -> {
                    byte[] questionBytes = packet.data().getBytes();
                    logger.debugf("[UDP] message received from %s:%s, length: %d", packet.sender().host(), packet.sender().port(), questionBytes.length);
                    final Responder responder = new UdpResponder(udpSocket, packet.sender().host(), packet.sender().port());
                    try {
                        final Message message = new Message(questionBytes);
                        // send event, wait for result
                        eventBus.send(Bus.CHECK_CACHE, new QueryContext(responder, message));
                    } catch (Exception ex) {
                        // send error to be handled
                        eventBus.send(Bus.HANDLE_ERROR, new QueryContext(responder, ex));
                    }
                });
            } else {
                logger.errorf("[UDP] Server listen failed on %s:%d - %s", SERVER_HOST, SERVER_PORT, asyncResult.cause());
            }
        });

    }

}
