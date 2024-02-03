package io.github.chrisruffalo.pintle.config.resolver;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.ResolverSourceType;
import io.github.chrisruffalo.pintle.dns.TlsVertxDnsClient;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;

/**
 * This resolver source provides additional configuration over
 * and beyond the TCP resolver to allow for TLS connections.
 */
@RegisterForReflection
public class TlsResolverSource extends TcpResolverSource {

    private String pem;

    @Override
    public ResolverSourceType type() {
        return ResolverSourceType.TLS;
    }

    public String getPem() {
        return pem;
    }

    public void setPem(String pem) {
        this.pem = pem;
    }

    @Override
    protected Resolver construct(PintleConfig config, io.github.chrisruffalo.pintle.config.Resolver resolverConfig) {
        Resolver create = super.construct(config, resolverConfig);
        if (create instanceof SimpleResolver simpleResolver) {
            final TlsVertxDnsClient vertxDnsClient = new TlsVertxDnsClient();
            vertxDnsClient.setTrustPem(this.getPem());
            simpleResolver.setClientFactory(vertxDnsClient);
        }
        return create;
    }
}
