package io.github.chrisruffalo.pintle.config.resolver;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.ResolverSourceType;
import org.xbill.DNS.Resolver;

/**
 * A source that uses a resolv.conf formatted file (https://man7.org/linux/man-pages/man5/resolv.conf.5.html)
 * to provide resolution. This allows the sytem resolver, or something like it, to be easily put in the
 * chain of resolvers.
 */
public class ResolvConfResolverSource extends BaseResolverSource {

    @Override
    public ResolverSourceType type() {
        return ResolverSourceType.CONF;
    }

    @Override
    protected Resolver construct(PintleConfig config, io.github.chrisruffalo.pintle.config.Resolver resolverConfig) {
        return null;
    }
}
