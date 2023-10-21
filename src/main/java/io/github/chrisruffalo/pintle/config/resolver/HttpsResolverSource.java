package io.github.chrisruffalo.pintle.config.resolver;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.ResolverSourceType;
import org.xbill.DNS.Resolver;

/**
 * Provides configuration and construction for a DNS over HTTP (DoH)
 * source that uses HTTPS.
 */
public class HttpsResolverSource extends BaseResolverSource {

    @Override
    public ResolverSourceType type() {
        return ResolverSourceType.HTTPS;
    }

    @Override
    protected Resolver construct(PintleConfig config, io.github.chrisruffalo.pintle.config.Resolver resolverConfig) {
        return null;
    }
}
