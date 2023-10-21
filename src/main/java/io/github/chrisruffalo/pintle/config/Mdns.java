package io.github.chrisruffalo.pintle.config;

import io.github.chrisruffalo.pintle.config.diff.Diff;
import io.github.chrisruffalo.pintle.config.diff.Diffable;
import io.smallrye.config.WithDefault;

import java.util.Collections;
import java.util.Set;

/**
 * Pintle can participate in MDNS especially
 * to look up client names on the network.
 */
public interface Mdns extends Diffable<Mdns> {

    /**
     * When this is true the MDNS feature will
     * be used.
     *
     * @return true to use mdns, false otherwise
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * What interfaces should pintle listen on
     * for MDNS. Using "all" is an alias that will
     * cause pintle to enumerate and listen on
     * all addresses.
     *
     * @return a set of interfaces to listen on or "all" to select all
     */
    @WithDefault("all")
    Set<String> interfaces();

    @Override
    default Diff diff(Mdns other) {
        return new Diff("mdns", Collections.emptySet());
    }

}
