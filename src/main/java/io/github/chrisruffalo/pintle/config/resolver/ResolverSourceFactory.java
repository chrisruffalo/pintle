package io.github.chrisruffalo.pintle.config.resolver;

import io.github.chrisruffalo.pintle.config.ResolverSource;
import io.github.chrisruffalo.pintle.util.NetUtil;
import io.github.chrisruffalo.pintle.util.PathUtil;
import io.github.chrisruffalo.pintle.util.UriUtil;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

/**
 * This class will take a string (udp://8.8.8.8:53) and parse it
 * into the appropriate resolver. This relies on custom schemes
 * that match the type (like "zone:///opt/zones/zone.db") and
 * other... creative interpretations of what it means to be
 * a URI.
 */
@RegisterForReflection
public class ResolverSourceFactory {

    public static ResolverSource create(final String uriString) {
        if (uriString == null || uriString.isEmpty()) {
            return null;
        }

        final Optional<URI> uriOptional = UriUtil.parse(uriString);
        if (uriOptional.isEmpty()) {
            // ??? all of these should pretty much be some
            //     sort of uri or at least translatable to
            return null;
        }
        final URI uri = uriOptional.get();

        String scheme = uri.getScheme();
        final String host = Optional.ofNullable(uri.getHost()).orElse(uriString);
        int port = uri.getPort();

        if (port < 1) {
            port = 53;
            if ("http".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("https".equalsIgnoreCase(scheme)) {
                port = 443;
            }
        }

        // this could basically mean only a few things
        // - a file source was given
        // - an ip was given
        // - a hostname was given (probably not correct)
        // - the person configuring it doesn't know what they are doing
        if (scheme == null) {
            final Optional<InetSocketAddress> ipOptional = NetUtil.fromString(host, port);
            if (ipOptional.isPresent()) {
                final UdpResolverSource udpResolverSource = new UdpResolverSource();
                udpResolverSource.setUri(uriString);
                return udpResolverSource;
            }

            final Optional<Path> pathOptional = PathUtil.find(PathUtil.real(uriString));
            if (pathOptional.isPresent()) {
                final Path path = pathOptional.get();
                BaseResolverSource base;
                if (path.endsWith(".db")) {
                    base = new ZonefileResolverSource();
                } else if (path.endsWith("conf")) {
                    base = new ResolvConfResolverSource();
                } else {
                    base = new HostfileResolverSource();
                }
                base.setUri(uriString);
                return base;
            }

            return null;
        }

        // base the rest on the configured schemes
        final BaseResolverSource source;
        switch (scheme.trim().toLowerCase()) {
            case "http":
                source = new HttpResolverSource();
                break;
            case "https":
                source = new HttpsResolverSource();
                break;
            case "tcp":
                source = new TcpResolverSource();
                break;
            case "tls":
                source = new TlsResolverSource();
                break;
            case "hosts":
                source = new HostfileResolverSource();
                break;
            case "zone":
                source = new ZonefileResolverSource();
                break;
            case "udp":
                source = new UdpResolverSource();
                break;
            case "resolv":
                source = new ResolvConfResolverSource();
                break;
            case "resolver":
                source = new ResolverResolverSource();
                break;
            default:
                source = null;
        }

        if (source != null) {
            // todo: might make it easier and break of the scheme portion here
            //       instead of letting each resolver figure it out or we might
            //       want to force each resolver to figure it out by itself.
            source.setUri(uriString);
        }


        return source;
    }


}
