package io.github.chrisruffalo.pintle.resolution.server;

import io.vertx.core.Future;
import io.vertx.core.datagram.DatagramSocket;

public class MdnsListenerHolder implements ListenerHolder<DatagramSocket> {

    final String name;

    final DatagramSocket datagramSocket;

    final String address;

    public MdnsListenerHolder(final String name, final DatagramSocket datagramSocket, final String address) {
        this.name = name;
        this.datagramSocket = datagramSocket;
        this.address = address;
    }

    @Override
    public DatagramSocket get() {
        return this.datagramSocket;
    }

    @Override
    public String name() {
        return name;
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
