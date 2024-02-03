package io.github.chrisruffalo.pintle.resolution.server;

import io.github.chrisruffalo.pintle.config.Listener;
import io.vertx.core.Future;
import io.vertx.core.net.NetServer;

public class TcpListenerHolder implements ListenerHolder<NetServer> {

    final Listener config;

    final NetServer netServer;

    final String address;

    public TcpListenerHolder(final Listener config, final NetServer netServer, final String address) {
        this.config = config;
        this.netServer = netServer;
        this.address = address;
    }


    @Override
    public String name() {
        return this.config.name();
    }

    @Override
    public NetServer get() {
        return netServer;
    }

    @Override
    public String address() {
        return this.address;
    }

    @Override
    public Future<Void> stop() {
        if (netServer != null) {
            return netServer.close();
        }
        return Future.failedFuture(new Exception("server is null"));
    }
}
