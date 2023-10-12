package io.github.chrisruffalo.pintle.resolution.server;

import io.github.chrisruffalo.pintle.config.Listener;
import io.github.chrisruffalo.pintle.model.ServiceType;
import io.vertx.core.Future;
import io.vertx.core.datagram.DatagramSocket;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Holds a server listener with additional metadata information
 */
public interface ListenerHolder {

    String name();

    Future<Void> stop();

}
