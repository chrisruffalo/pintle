package io.github.chrisruffalo.pintle.resolution.resolver;

import io.github.chrisruffalo.pintle.config.Group;
import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.Resolver;
import io.github.chrisruffalo.pintle.config.producer.ConfigProducer;
import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.event.ConfigUpdate;
import io.quarkus.vertx.ConsumeEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class ResolverHandler {

    private static final int DEFAULT_PORT = 53;

    @Inject
    ConfigProducer configProducer;

    @Inject
    Logger logger;

    final Map<String, PintleResolver> resolverMap = new HashMap<>();

    @ConsumeEvent(Bus.CONFIG_UPDATE_RESOLVERS)
    public void configure(ConfigUpdate event) {
        if (!event.isInitial()) {
            logger.debugf("logging subsystem config update %s", event.getId());
        }
        final PintleConfig config = configProducer.get(event.getId());
        resolverMap.clear();
        if (config.resolvers().isEmpty()) {
            return;
        }
        for (final Resolver resolverConfig : config.resolvers().get()) {
            final PintleResolver resolver = resolverConfig.resolver(config);
            if (resolver == null) {
                continue;
            }
            resolverMap.put(resolverConfig.name(), resolver);
        }
    }

    public List<PintleResolver> get(final Group group) {
           if (group.resolvers().isEmpty()) {
               return Collections.emptyList();
           }
           List<PintleResolver> resolvers = group.resolvers().get().stream().map(resolverMap::get).collect(Collectors.toList());
           logger.debugf("found %d resolvers for group %s (listed %d)", new Object[]{resolvers.size(), group.name(), group.resolvers().get().size()});
           return  resolvers;
    }

}
