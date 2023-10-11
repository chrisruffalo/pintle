package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.ResolverType;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

@ApplicationScoped
public class ResolverHandler {

    private static final int DEFAULT_PORT = 53;

    @Inject
    PintleConfig config;

    @Inject
    Logger logger;

    Resolver parent;

    public void configure(@Observes StartupEvent startupEvent) {
        final List<Resolver> configured = new LinkedList<>();

        for(io.github.chrisruffalo.pintle.config.Resolver configuredResolver : config.resolvers()) {
            if (ResolverType.TCP.equals(configuredResolver.type()) || ResolverType.UDP.equals(configuredResolver.type())) {
                final ExtendedResolver extendedResolver = new ExtendedResolver();
                for (String source : configuredResolver.sources()) {
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
                    if (ResolverType.TCP.equals(configuredResolver.type())) {
                        resolver.setTCP(true);
                    }
                    extendedResolver.addResolver(resolver);
                }
                logger.infof("configured resolver: %s", configuredResolver.name());
                configured.add(extendedResolver);
            }
        }
        // combine resolvers
        final ExtendedResolver resolver = new ExtendedResolver();
        configured.forEach(resolver::addResolver);
        resolver.setLoadBalance(true);
        resolver.setIgnoreTruncation(false);
        resolver.setRetries(3);

        // set resolution
        parent = resolver;
    }

    @Produces
    public Resolver provide() {
        return parent;
    }

}
