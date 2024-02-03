package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Responsible for mapping a client address to a client name that is
 * friendly for the UI user and also that can be used to match to them.
 */
@ApplicationScoped
public class ClientNameController {

    @Inject
    Logger logger;

    @Inject
    EventBus eventBus;

    @WithSpan("assign client name")
    @ConsumeEvent(Bus.ASSIGN_CLIENT_NAME)
    @Transactional
    @RunOnVirtualThread
    void map(final QueryContext context) {

        // get names for the client address
        final String clientAddress = context.getResponder().toClient();

        eventBus.send(Bus.ASSIGN_GROUP, context);
    }

}
