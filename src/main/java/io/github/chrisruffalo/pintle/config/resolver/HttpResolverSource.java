package io.github.chrisruffalo.pintle.config.resolver;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.ResolverSourceType;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.xbill.DNS.Resolver;

/**
 * Provides configuration and construction for a DNS over HTTP (DoH)
 * source that does not use HTTPS.
 */
@RegisterForReflection
public class HttpResolverSource extends BaseResolverSource {

    @Override
    public ResolverSourceType type() {
        return ResolverSourceType.HTTP;
    }

    @Override
    protected Resolver construct(PintleConfig config, io.github.chrisruffalo.pintle.config.Resolver resolverConfig) {
        return null;
    }
}
