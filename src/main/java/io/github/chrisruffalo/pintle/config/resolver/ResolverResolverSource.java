package io.github.chrisruffalo.pintle.config.resolver;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.ResolverSourceType;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.xbill.DNS.Resolver;

/**
 * Allows a resolver to link to other resolvers in a hierarchy. This means
 * that this source will call into another resolver by name.
 */
@RegisterForReflection
public class ResolverResolverSource extends BaseResolverSource {

    @Override
    public ResolverSourceType type() {
        return ResolverSourceType.RESOLVER;
    }

    @Override
    protected Resolver construct(PintleConfig config, io.github.chrisruffalo.pintle.config.Resolver resolverConfig) {
        return null;
    }
}
