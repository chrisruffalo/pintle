package io.github.chrisruffalo.pintle.util;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;

import java.net.InetSocketAddress;
import java.util.Optional;

public class NetUtil {

    public static Optional<IPAddress> fromString(final String given) {
        if (given == null || given.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new IPAddressString(given).toAddress());
        } catch (Exception ex) {
             // no-op
        }
        return Optional.empty();
    }

    public static Optional<InetSocketAddress> fromString(final String given, int defaultPort) {
        if (given == null || given.trim().isEmpty()) {
            return Optional.empty();
        }

        // split uri
        final String[] split = given.split(":");

        // create inet
        Optional<IPAddress> inetAddressOptional = NetUtil.fromString(split[0]);
        if (inetAddressOptional.isEmpty()) {
            return Optional.empty();
        }

        InetSocketAddress inetSocketAddress;
        if (split.length == 2) {
            inetSocketAddress = new InetSocketAddress(inetAddressOptional.get().toInetAddress(), NumberUtil.safeInt(split[1], defaultPort));
        } else {
            inetSocketAddress = new InetSocketAddress(inetAddressOptional.get().toInetAddress(), defaultPort);
        }

        return Optional.of(inetSocketAddress);
    }

}
