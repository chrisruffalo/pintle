package io.github.chrisruffalo.pintle.config.resolver;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.ResolverSourceType;
import io.github.chrisruffalo.pintle.dns.TlsVertxDnsClient;
import io.github.chrisruffalo.pintle.resolution.resolver.MdnsResolver;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;

/**
 * This resolver source provides additional configuration over
 * and beyond the TCP resolver to allow for TLS connections.
 */
@RegisterForReflection
public class MdnsResolverSource extends BaseResolverSource {

    @Override
    public ResolverSourceType type() {
        return ResolverSourceType.MDNS;
    }

    @Override
    protected Resolver construct(PintleConfig config, io.github.chrisruffalo.pintle.config.Resolver resolverConfig) {
        return new MdnsResolver(config, resolverConfig);
    }

}
