package io.github.chrisruffalo.pintle.config.resolver;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.ResolverSourceType;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;

/**
 * This resolver source provides additional configuration over
 * and beyond the TCP resolver to allow for TLS connections.
 */
public class TlsResolverSource extends TcpResolverSource {

    @Override
    public ResolverSourceType type() {
        return ResolverSourceType.TLS;
    }

    @Override
    protected Resolver construct(PintleConfig config, io.github.chrisruffalo.pintle.config.Resolver resolverConfig) {
        return super.construct(config, resolverConfig);
    }
}
