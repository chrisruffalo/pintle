package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.config.Group;
import io.github.chrisruffalo.pintle.config.Matcher;
import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.producer.ConfigProducer;
import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.event.ConfigUpdate;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class GroupController {

    private static final String DEFAULT_GROUP_NAME = "default";

    private static final Group DEFAULT = new Group() {
        @Override
        public String name() {
            return DEFAULT_GROUP_NAME;
        }

        @Override
        public Optional<List<String>> resolvers() {
            return Optional.empty();
        }

        @Override
        public Optional<List<String>> lists() {
            return Optional.empty();
        }

        @Override
        public Optional<List<Matcher>> matchers() {
            return Optional.empty();
        }
    };

    @Inject
    ConfigProducer configProducer;

    @Inject
    EventBus bus;

    @Inject
    Logger logger;

    @ConsumeEvent(Bus.CONFIG_UPDATE_GROUPS)
    public void configure(ConfigUpdate event) {

    }

    @WithSpan("assign to group")
    @ConsumeEvent(Bus.ASSIGN_GROUP)
    @RunOnVirtualThread
    public void assignToGroup(QueryContext context) {
        final PintleConfig config = configProducer.get(context.getConfigId());
        context.setGroup(DEFAULT);

        // get the first matching group. first match wins.
        if (config.groups().isPresent() && !config.groups().get().isEmpty()) {
            for (final Group group : config.groups().get()) {
                // use the default group configured inside the list of groups
                if (DEFAULT_GROUP_NAME.equals(group.name())) {
                    context.setGroup(group);
                    continue;
                }
                // otherwise match the first matching group
                if(group.matches(context)) {
                    context.setGroup(group);
                    break;
                }
            }
        }

        logger.debugf("[%s] mapped query to group '%s'", context.getTraceId(), context.getGroup().name());
        bus.send(Bus.CHECK_CACHE, context);
    }
}
