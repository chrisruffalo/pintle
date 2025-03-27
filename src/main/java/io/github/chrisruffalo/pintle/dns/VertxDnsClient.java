package io.github.chrisruffalo.pintle.dns;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import org.jboss.logging.Logger;
import org.xbill.DNS.Message;
import org.xbill.DNS.io.IoClientFactory;
import org.xbill.DNS.io.TcpIoClient;
import org.xbill.DNS.io.UdpIoClient;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

public class VertxDnsClient implements TcpIoClient, UdpIoClient, IoClientFactory {

    private static final int EPHEMERAL_RANGE_START = 1024;
    private static final int EPHEMERAL_RANGE_END = EPHEMERAL_RANGE_START + 1024;

    private final Logger logger = Logger.getLogger(this.getClass());

    @Override
    public TcpIoClient createOrGetTcpClient() {
        return this;
    }

    @Override
    public UdpIoClient createOrGetUdpClient() {
        return this;
    }

    protected NetClientOptions createOptions() {
        final NetClientOptions options = new NetClientOptions();
        options.setTcpFastOpen(true);
        options.setTcpKeepAlive(true);
        options.setTcpNoDelay(true);
        return options;
    }

    @Override
    public CompletableFuture<byte[]> sendAndReceiveTcp(InetSocketAddress localAddress, InetSocketAddress remoteAddress, Message message, byte[] bytes, Duration duration) {
        final Vertx vertx = Vertx.currentContext().owner();
        final NetClientOptions options = createOptions();

        final NetClient client = vertx.createNetClient(options);

        final CompletableFuture<byte[]> resultFuture = new CompletableFuture<>();

        client.connect(remoteAddress.getPort(), remoteAddress.getAddress().getHostAddress(), res -> {
            if (res.succeeded()) {
                final NetSocket socket = res.result();

                // Handle data received from the server
                socket.handler(buffer -> {
                    final byte[] lengthBytes = buffer.getBytes(0,2);
                    final int expectedLength = ((lengthBytes[0] & 0xff) << 8) | (lengthBytes[1] & 0xff);
                    final byte[] questionBytes = buffer.getBytes(2, buffer.length());
                    logger.debugf("received response of %d bytes", expectedLength);
                    resultFuture.complete(questionBytes);
                    socket.end();
                });

                // Handle connection errors
                socket.exceptionHandler(e -> {
                    resultFuture.completeExceptionally(e);
                    socket.end();
                });

                // add the length to the message
                final Buffer output = Buffer.buffer();
                output.appendByte((byte) (bytes.length >>> 8));
                output.appendByte((byte) (bytes.length & 0xFF));
                output.appendBytes(bytes);

                // Write data to the server
                socket.write(output);
            } else {
                resultFuture.completeExceptionally(res.cause());
            }
        });

        // Set a timeout for the future
        vertx.setTimer(duration.toMillis(), timerId -> {
            client.close();
            resultFuture.completeExceptionally(new TimeoutException("Timeout exceeded"));
        });

        return resultFuture.thenApply(responseBytes -> {
            return responseBytes;
        });
    }

    private CompletableFuture<byte[]> listenAndSend(DatagramSocket client, int port, InetSocketAddress localAddress, InetSocketAddress remoteAddress, Message message, byte[] bytes, int i, Duration duration) {
        final CompletableFuture<byte[]> completableFuture = new CompletableFuture<>();

        if (port > EPHEMERAL_RANGE_END) {
            completableFuture.completeExceptionally(new Exception("Ran out of ephemeral ports to listen on"));
            return completableFuture;
        }

        if (localAddress == null) {
            localAddress = new InetSocketAddress(port);
        }

        // create accessible final copy
        final InetSocketAddress finalLocalAddress = localAddress;

        client.listen(port, localAddress.getAddress().getHostAddress(), listenHandler -> {
            if(listenHandler.succeeded()) {

                final DatagramSocket listener = listenHandler.result();
                listener.handler(dataHandler -> {
                    logger.tracef("received response on ephemeral port %d", port);
                    completableFuture.complete(dataHandler.data().getBytes());
                    client.close();
                });

                logger.tracef("listening for responses on ephemeral port %d", port);

                listener.exceptionHandler(e -> {
                    completableFuture.completeExceptionally(e);
                    client.close();
                });

                listener.send(Buffer.buffer(bytes), remoteAddress.getPort(), remoteAddress.getAddress().getHostAddress());
            } else {
                final CompletableFuture<byte[]> internalResult = listenAndSend(client, port + 1, finalLocalAddress, remoteAddress, message, bytes, i, duration);
                internalResult.whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        completableFuture.completeExceptionally(throwable);
                    } else {
                        completableFuture.complete(result);
                    }
                });
            }
        });

        return completableFuture;
    }

    @Override
    public CompletableFuture<byte[]> sendAndReceiveUdp(InetSocketAddress localAddress, InetSocketAddress remoteAddress, Message message, byte[] bytes, int i, Duration duration) {
        logger.debugf("sending udp request");

        final DatagramSocketOptions options = new DatagramSocketOptions();
        final DatagramSocket client = Vertx.currentContext().owner().createDatagramSocket(options);

        final CompletableFuture<byte[]> completableFuture = listenAndSend(client, EPHEMERAL_RANGE_START, localAddress, remoteAddress, message, bytes, i, duration);

        final Vertx vertx = Vertx.currentContext().owner();
        // Set a timeout for the future
        vertx.setTimer(duration.toMillis(), timerId -> {
            client.close();
            completableFuture.completeExceptionally(new TimeoutException("Timeout exceeded"));
        });

        return completableFuture;
    }
}
