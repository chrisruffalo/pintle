package io.github.chrisruffalo.pintle.config.resolver;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.ResolverSourceType;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.xbill.DNS.Resolver;

/**
 * Configures reading of BIND-style zone files. The format of
 * these files is described in RFC-1035 (https://datatracker.ietf.org/doc/html/rfc1035)
 *
 */
@RegisterForReflection
public class ZonefileResolverSource extends BaseResolverSource {

    @Override
    public ResolverSourceType type() {
        return ResolverSourceType.ZONE;
    }

    @Override
    protected Resolver construct(PintleConfig config, io.github.chrisruffalo.pintle.config.Resolver resolverConfig) {
        return null;
    }
}
