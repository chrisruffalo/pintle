package io.github.chrisruffalo.pintle.resolution.server;

import io.github.chrisruffalo.pintle.config.Listener;
import io.vertx.core.Future;
import io.vertx.core.datagram.DatagramSocket;

public class UdpListenerHolder implements ListenerHolder {

    final Listener config;

    final DatagramSocket datagramSocket;

    public UdpListenerHolder(final Listener config, final DatagramSocket datagramSocket) {
        this.config = config;
        this.datagramSocket = datagramSocket;
    }

    @Override
    public String name() {
        return this.config.name();
    }

    @Override
    public Future<Void> stop() {
        if (datagramSocket != null) {
            return datagramSocket.close();
        }
        return Future.failedFuture(new Exception("server is null"));
    }
}
