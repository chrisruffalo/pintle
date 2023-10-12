package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.config.Group;
import io.github.chrisruffalo.pintle.config.Matcher;
import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class GroupController {

    private static final String DEFAULT_GROUP_NAME = "default";

    @Inject
    PintleConfig config;

    @Inject
    EventBus bus;

    @Inject
    Logger logger;

    private Group defaultGroup = null;
    private final Map<String, Group> groups = new HashMap<>();

    public void configure(@Observes StartupEvent startupEvent) {
        // go through the config if available
        if (config.groups().isPresent() || !config.groups().get().isEmpty()) {
            for (final Group group : config.groups().get()) {
                groups.put(group.name(), group);
            }
        }

        // create the default group and ensure it is added to the
        // map if it is null
        defaultGroup = groups.computeIfAbsent(DEFAULT_GROUP_NAME, k -> createDefaultGroup());

        // now that we know we have a default group we can go back through
        // the list of groups and establish them all and build the matcher
        // tree for each one

    }

    /**
     * Get a group by name. If no group is given return the default.
     *
     * @param name of the group to get
     * @return the configured group if found, the default group otherwise
     */
    public Group getGroupByName(final String name) {
        return groups.getOrDefault(name, defaultGroup);
    }

    @WithSpan("assign to group")
    @ConsumeEvent(Bus.ASSIGN_GROUP)
    @RunOnVirtualThread
    public void assignToGroup(QueryContext context) {
        context.setGroup(defaultGroup);

        // get the first matching group. first match wins.
        if (config.groups().isPresent() && !config.groups().get().isEmpty()) {
            for (final Group group : config.groups().get()) {
                if(group.matches(context)) {
                    context.setGroup(group);
                    break;
                }
            }
        }
        logger.debugf("[%s] mapped query to group '%s'", context.getTraceId(), context.getGroup().name());
        bus.send(Bus.CHECK_CACHE, context);
    }

    private Group createDefaultGroup() {
        return new Group() {
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
    }

}
