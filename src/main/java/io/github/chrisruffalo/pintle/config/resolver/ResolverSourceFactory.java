package io.github.chrisruffalo.pintle.config.resolver;

import io.github.chrisruffalo.pintle.config.ResolverSource;

/**
 * This class will take a string (udp://8.8.8.8:53) and parse it
 * into the appropriate resolver. This relies on custom schemes
 * that match the type (like "zone:///opt/zones/zone.db") and
 * other... creative interpretations of what it means to be
 * a URI.
 */
public class ResolverSourceFactory {

    public static ResolverSource create(final String uri) {
        if (uri == null || uri.isEmpty()) {
            return null;
        }



        return null;
    }


}
