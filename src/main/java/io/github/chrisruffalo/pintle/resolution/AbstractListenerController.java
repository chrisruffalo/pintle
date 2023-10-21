package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.producer.ConfigProducer;
import io.github.chrisruffalo.pintle.resolution.server.ListenerHolder;
import io.opentelemetry.api.trace.Tracer;
import io.quarkus.runtime.ShutdownEvent;
import io.vertx.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractListenerController {

    @Inject
    ConfigProducer configProducer;

    PintleConfig config;

    @Inject
    Logger logger;

    @Inject
    Vertx vertx;

    @Inject
    EventBus eventBus;

    @Inject
    Tracer tracer;

    protected final List<ListenerHolder> listeners = new LinkedList<>();

    public void stopServers(@Observes ShutdownEvent shutdownEvent) {
        for (final ListenerHolder server : this.listeners) {
            // services with unspecified types go here
            if (server == null) {
                continue;
            }

            server.stop().onComplete(handler -> {
                if (handler.succeeded()) {
                    logger.infof("shutdown server %s", server.name());
                } else if(handler.cause() != null) {
                    logger.infof("failed to shutdown server %s: %s", server.name(), handler.cause().getMessage());
                } else {
                    logger.infof("failed to shutdown server %s: %s", server.name(), handler.cause().getMessage());
                }
            }).result();
        }
    }


}
