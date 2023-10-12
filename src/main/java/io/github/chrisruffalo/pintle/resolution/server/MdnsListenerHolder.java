package io.github.chrisruffalo.pintle.resolution.server;

import io.vertx.core.Future;
import io.vertx.core.datagram.DatagramSocket;

public class MdnsListenerHolder implements ListenerHolder {

    final String name;

    final DatagramSocket datagramSocket;

    public MdnsListenerHolder(final String name, final DatagramSocket datagramSocket) {
        this.name = name;
        this.datagramSocket = datagramSocket;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Future<Void> stop() {
        if (datagramSocket != null) {
            return datagramSocket.close();
        }
        return Future.failedFuture(new Exception("server is null"));
    }
}
