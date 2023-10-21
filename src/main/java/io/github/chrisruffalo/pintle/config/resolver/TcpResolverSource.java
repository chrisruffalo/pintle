package io.github.chrisruffalo.pintle.config.resolver;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.ResolverSourceType;
import org.xbill.DNS.Resolver;

/**
 * Provides configuration for TCP-based DNS clients. This client
 * is based on the UDP source and provides additional configuration
 * for TCP.
 */
public class TcpResolverSource extends UdpResolverSource {

    @Override
    public ResolverSourceType type() {
        return ResolverSourceType.TCP;
    }

    @Override
    protected Resolver construct(PintleConfig config, io.github.chrisruffalo.pintle.config.Resolver resolverConfig) {
        final Resolver resolver = super.construct(config, resolverConfig);
        resolver.setTCP(true);
        return resolver;
    }
}
