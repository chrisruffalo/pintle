package io.github.chrisruffalo.pintle.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.chrisruffalo.pintle.config.diff.Diffable;
import io.github.chrisruffalo.pintle.config.resolver.*;
import io.github.chrisruffalo.pintle.config.serde.ResolverSourceConverter;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import org.xbill.DNS.Resolver;

/**
 * A resolver source represents an item that is able to make
 * an actual query. It can be a local or remote source but
 * either way the resolver source is able to turn a hostname
 * and a query type into an ip or set of ips.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = UdpResolverSource.class)
@JsonSubTypes(value = {
    @JsonSubTypes.Type(value = UdpResolverSource.class, name = "udp", names = {"udp", "UDP"}),
    @JsonSubTypes.Type(value = TcpResolverSource.class, name = "tcp", names = {"tcp", "TCP"}),
    @JsonSubTypes.Type(value = TlsResolverSource.class, name = "tls", names = {"tls", "TLS"}),
    @JsonSubTypes.Type(value = HttpResolverSource.class, name = "http", names = {"http", "HTTP"}),
    @JsonSubTypes.Type(value = HttpsResolverSource.class, name = "https", names = {"https", "HTTPS"}),
    @JsonSubTypes.Type(value = ResolverResolverSource.class, name = "resolver"),
    @JsonSubTypes.Type(value = ResolvConfResolverSource.class, name = "conf", names = {"conf", "resolv.conf"}),
    @JsonSubTypes.Type(value = HostfileResolverSource.class, name = "hostfile", names = {"hostfile", "hosts"}),
    @JsonSubTypes.Type(value = HostfileResolverSource.class, name = "zone", names = {"zone", "zonefile", "bind"}),
})
@JsonIgnoreProperties(ignoreUnknown = true)
@WithConverter(ResolverSourceConverter.class)
public interface ResolverSource extends Diffable<ResolverSource>, Comparable<ResolverSource> {

    /**
     * The uri (address, port, disk location, etc) that the source
     * will interpret to find DNS information.
     *
     * @return the uri string
     */
    String uri();

    /**
     * The type of resolver source.
     *
     * @return type of the resolver source.
     */
    @WithDefault("udp")
    ResolverSourceType type();

    /**
     * Consturcts the resolver object that will be able
     * to make the connection/lookup.
     *
     * @param config root config
     * @param resolverConfig config for the owning/parent resolver
     * @return a constructed resolver instance
     */
    @JsonIgnore
    default Resolver resolver(final PintleConfig config, final io.github.chrisruffalo.pintle.config.Resolver resolverConfig) {
        return null;
    };


    @Override
    default int compareTo(ResolverSource o) {
        return 0;
    }
}
