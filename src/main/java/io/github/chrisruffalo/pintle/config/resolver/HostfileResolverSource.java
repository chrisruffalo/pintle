package io.github.chrisruffalo.pintle.config.resolver;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.ResolverSourceType;
import org.xbill.DNS.Resolver;

/**
 * Represents a hostfile-based source. In the terms of Pintle
 * a hostfile is a file like /etc/hosts taht consists of
 * two columns of data: the first column the address to resolve
 * and the second column the hostname to resolve to the address.
 */
public class HostfileResolverSource extends BaseResolverSource {

    @Override
    public ResolverSourceType type() {
        return ResolverSourceType.HOSTFILE;
    }

    @Override
    protected Resolver construct(PintleConfig config, io.github.chrisruffalo.pintle.config.Resolver resolverConfig) {
        return null;
    }
}
