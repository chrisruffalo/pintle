package io.github.chrisruffalo.pintle.resolution.server;

import io.vertx.core.Future;

/**
 * Holds a server listener with additional metadata information
 */
public interface ListenerHolder<C> {

    String name();

    String address();

    C get();

    Future<Void> stop();

}
