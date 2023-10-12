package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.config.Group;
import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ResolverHandler {

    private static final int DEFAULT_PORT = 53;

    @Inject
    PintleConfig config;

    @Inject
    Logger logger;

    private final Map<String, Resolver> nameToResolverMap = new HashMap<>();

    private final Map<String, Resolver> groupNameToResolvers = new HashMap<>();

    private Resolver configureUdpResolver(io.github.chrisruffalo.pintle.config.Resolver configuration) {
        final List<Resolver> resolvers = new LinkedList<>();
        for (String source : configuration.sources()) {
            final String[] split = source.split(":");
            int port = DEFAULT_PORT;
            if (split.length > 1) {
                try {
                    port = Integer.parseInt(split[1]);
                } catch (Exception e) {
                    // does not matter
                }
            }
            final InetSocketAddress socketAddress = new InetSocketAddress(split[0], port);
            final Resolver resolver = new SimpleResolver(socketAddress);
            resolver.setEDNS(0, 1200, 0);
            resolvers.add(resolver);
        }
        return new ExtendedResolver(resolvers);
    }


    private Resolver configureTcpResolver(io.github.chrisruffalo.pintle.config.Resolver configuration) {
        Resolver resolver = configureUdpResolver(configuration);
        resolver.setTCP(true);
        return resolver;
    }

    public void configure(@Observes StartupEvent startupEvent) {
        if (config.resolvers().isEmpty()) {
            return;
        }
        final List<io.github.chrisruffalo.pintle.config.Resolver> doAfter = new LinkedList<>();

        for(io.github.chrisruffalo.pintle.config.Resolver configuredResolver : config.resolvers().get()) {
            final Resolver r = switch (configuredResolver.type()) {
                case UDP -> configureUdpResolver(configuredResolver);
                case TCP -> configureTcpResolver(configuredResolver);
                case RESOLVER -> {
                    doAfter.add(configuredResolver);
                    yield null;
                }
                case ZONE -> null;
                case FILE -> null;
            };
            if (r != null) {
                nameToResolverMap.put(configuredResolver.name(), r);
            }
        }

        // once all the other types are resolved then we can resolve
        // resolvers that reference other resolvers
        for (io.github.chrisruffalo.pintle.config.Resolver after : doAfter) {
            List<Resolver> candidates = new LinkedList<>();
            for (final String resolverSource : after.sources()) {
                final Resolver r = nameToResolverMap.get(resolverSource);
                if (r != null) {
                    candidates.add(r);
                }
            }
            if (!candidates.isEmpty()) {
                final ExtendedResolver parentResolver = new ExtendedResolver(candidates);
                parentResolver.setLoadBalance(after.balance());
                parentResolver.setIgnoreTruncation(false);
                parentResolver.setRetries(candidates.size());
                nameToResolverMap.put(after.name(), parentResolver);
            }
        }
    }

    public Resolver get(final Group group) {
        // cannot resolve when no resolver is found
        if (group.resolvers().isEmpty() || group.resolvers().get().isEmpty()) {
            return null;
        }
        List<String> groupResolvers = group.resolvers().get();
        if (groupResolvers.size() == 1) {
            return nameToResolverMap.get(groupResolvers.get(0));
        }
        List<Resolver> foundNamedResolvers = new LinkedList<>();
        for(final String resolverName : groupResolvers) {
            if(nameToResolverMap.containsKey(resolverName)) {
                foundNamedResolvers.add(nameToResolverMap.get(resolverName));
            }
        }

        // we need to construct and save resolver for group
        if (!foundNamedResolvers.isEmpty()) {
            return groupNameToResolvers.computeIfAbsent(group.name(), k -> {
                final ExtendedResolver parentResolver = new ExtendedResolver(foundNamedResolvers);
                parentResolver.setRetries(foundNamedResolvers.size());
                return parentResolver;
            });
        }

        return null;
    }

}
