package io.github.chrisruffalo.pintle.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.chrisruffalo.pintle.config.diff.Diff;
import io.github.chrisruffalo.pintle.config.diff.Diffable;
import io.github.chrisruffalo.pintle.config.serde.ResolverSourceConverter;
import io.github.chrisruffalo.pintle.resolution.resolver.PintleResolver;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

import java.util.*;

/**
 * A resolver represents a collection of one or more
 * sources to use when resolving a query. Each resolver
 * has a list of sources that it will consult.
 *
 */
public interface Resolver extends Named, Diffable<Resolver> {


    /**
     * If true this will balance connections, via round-robin
     * selection, between sources. If no resolution is found
     * then the next available source will be selected.
     *
     * If this is false then the sources will be tried, in order,
     * until a resolution is found.
     *
     *
     * @return if outbound connections should be balanced
     */
    @WithDefault("false")
    boolean balance();

    /**
     * The list of sources that this resolver should
     * use to resolve queries.
     *
     * @return the list of sources if available, empty if not
     */
    @WithDefault("")
    @WithConverter(ResolverSourceConverter.class)
    Optional<List<ResolverSource>> sources();

    /**
     * A resolver can be configured to only respond for certain domains.
     * This is useful for things like ISP specific functionality or
     * for managing local domains that might overlap with external
     * domains.
     *
     * @return the list of domains serviced by this resolver, empty if all domains should be tried.
     */
    @WithDefault("")
    Optional<Set<String>> domains();

    /**
     * Constructs a resolver holder that can handle
     * queries directly.
     *
     * @param config the root config for additional context if needed
     * @return a constructed resolver
     */
    @JsonIgnore
    default PintleResolver resolver(final PintleConfig config) {
        final List<org.xbill.DNS.Resolver> resolvers = new LinkedList<>();
        return new PintleResolver(config, this);
    }

    @Override
    default Diff diff(Resolver other) {
        return new Diff("resolver", Collections.emptySet());
    }

}
