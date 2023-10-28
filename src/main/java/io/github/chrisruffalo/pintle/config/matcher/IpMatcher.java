package io.github.chrisruffalo.pintle.config.matcher;

import inet.ipaddr.IPAddress;
import io.github.chrisruffalo.pintle.config.MatcherType;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.github.chrisruffalo.pintle.util.NetUtil;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Optional;

/**
 * Matches against the client ip
 */
@RegisterForReflection
public class IpMatcher extends StringValuesMatcher {

    @Override
    public MatcherType type() {
        return MatcherType.IP;
    }

    @Override
    public boolean match(QueryContext against) {
        // get client ip from query context
        final Optional<IPAddress> clientOptional = NetUtil.fromString(against.getResponder().toClient());
        if (clientOptional.isEmpty()) {
            return false;
        }
        final IPAddress client = clientOptional.get();
        for(final String value : this.getValues()) {
            final Optional<IPAddress> ip = NetUtil.fromString(value);
            if (ip.isEmpty()) {
                continue;
            }
            IPAddress address = ip.get();
            if (client.isIPv4() && !address.isIPv4() && address.isIPv4Convertible()) {
                address = address.toIPv4();
            } else if(client.isIPv6() && !address.isIPv6() && address.isIPv6Convertible()) {
                address = address.toIPv6();
            }
            return address.equals(client);
        }
        return false;
    }
}
