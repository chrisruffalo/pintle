package io.github.chrisruffalo.pintle.resolution.server;

import io.github.chrisruffalo.pintle.config.Listener;
import io.vertx.core.Future;
import io.vertx.core.net.NetServer;

public class TcpListenerHolder implements ListenerHolder {

    final Listener config;

    final NetServer netServer;

    public TcpListenerHolder(final Listener config, final NetServer netServer) {
        this.config = config;
        this.netServer = netServer;
    }


    @Override
    public String name() {
        return this.config.name();
    }

    @Override
    public Future<Void> stop() {
        if (netServer != null) {
            return netServer.close();
        }
        return Future.failedFuture(new Exception("server is null"));
    }
}
