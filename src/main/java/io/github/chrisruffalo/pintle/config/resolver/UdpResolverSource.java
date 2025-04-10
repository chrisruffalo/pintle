package io.github.chrisruffalo.pintle.config.resolver;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.ResolverSourceType;
import io.github.chrisruffalo.pintle.dns.VertxDnsClient;
import io.github.chrisruffalo.pintle.util.NetUtil;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * Configures and constructs a UDP client using the dnsjava
 * SimpleResolver.
 */
@RegisterForReflection
public class UdpResolverSource extends BaseResolverSource {

    @Override
    public ResolverSourceType type() {
        return ResolverSourceType.UDP;
    }

    @Override
    protected Resolver construct(PintleConfig config, io.github.chrisruffalo.pintle.config.Resolver resolverConfig) {
        Optional<InetSocketAddress> socketAddress = NetUtil.fromString(this.uri(), 53);
        final SimpleResolver sr = socketAddress.map(SimpleResolver::new).orElse(null);
        if (sr != null) {
            // something is wrong with the VertxDnsClient and it isn't working as expected, the question
            // is apparently sent but no response ever is forwarded to the rest of the system or never
            // returns in the first place
            sr.setIoClientFactory(new VertxDnsClient());
        }
        return sr;
    }
}
