package io.github.chrisruffalo.pintle.resolution.server;

import io.github.chrisruffalo.pintle.config.Listener;
import io.vertx.core.Future;
import io.vertx.core.datagram.DatagramSocket;

public class UdpListenerHolder implements ListenerHolder<DatagramSocket> {

    final Listener config;

    final DatagramSocket datagramSocket;

    final String address;

    public UdpListenerHolder(final Listener config, final DatagramSocket datagramSocket, final String address) {
        this.config = config;
        this.datagramSocket = datagramSocket;
        this.address = address;
    }

    @Override
    public DatagramSocket get() {
        return this.datagramSocket;
    }

    @Override
    public String name() {
        return this.config.name();
    }

    @Override
    public String address() {
        return this.address;
    }

    @Override
    public Future<Void> stop() {
        if (datagramSocket != null) {
            return datagramSocket.close();
        }
        return Future.failedFuture(new Exception("server is null"));
    }
}
