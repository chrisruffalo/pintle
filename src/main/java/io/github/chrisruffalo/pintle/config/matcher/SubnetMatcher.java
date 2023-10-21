package io.github.chrisruffalo.pintle.config.matcher;

import inet.ipaddr.IPAddress;
import io.github.chrisruffalo.pintle.config.MatcherType;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.github.chrisruffalo.pintle.util.NetUtil;

import java.util.Optional;

/**
 * Matches against the client ip if it is within
 * a given subnet.
 */
public class SubnetMatcher extends StringValuesMatcher {

    @Override
    public MatcherType type() {
        return MatcherType.SUBNET;
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
            final Optional<IPAddress> rangeOptional = NetUtil.fromString(value);
            if (rangeOptional.isEmpty()) {
                continue;
            }
            IPAddress range = rangeOptional.get();
            if (client.isIPv6() && !range.isIPv6() && range.isIPv6Convertible()) {
                range = range.toIPv6();
            }
            if (client.isIPv4() && !range.isIPv4() && range.isIPv4Convertible()) {
                range = range.toIPv4();
            }
            if (
                range.contains(client)
             || (range.isIPv4() && client.isIPv4Convertible() && range.contains(client.toIPv4()))
             || (range.isIPv6() && client.isIPv6Convertible() && range.contains(client.toIPv6()))
            ) {
                return true;
            }

        }
        return false;
    }
}
