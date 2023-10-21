package io.github.chrisruffalo.pintle.config;

import io.github.chrisruffalo.pintle.config.diff.Diff;
import io.github.chrisruffalo.pintle.config.diff.Diffable;
import io.github.chrisruffalo.pintle.config.diff.StringDiffable;
import io.github.chrisruffalo.pintle.config.serde.ServiceTypeConverter;
import io.github.chrisruffalo.pintle.model.ServiceType;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A listener represents how DNS traffic makes it into Pintle for resolution. This
 * can describe listeners of different types, addresses, and ports that all
 * feed into Pintle for resolution.
 */
public interface Listener extends Diffable<Listener>, NamedComparable<Listener> {

    /**
     * The name used to refer to the listener
     *
     * @return reference name of the listener
     */
    String name();

    /**
     * The type of listener, see {@link ServiceType}
     *
     * @return the type of the listener
     */
    @WithDefault("UDP")
    @WithConverter(ServiceTypeConverter.class)
    ServiceType type();

    /**
     * Properly formatted IP address of the server
     *
     * @return a set of addresses representing the interfaces this listener should bind to
     */
    Optional<Set<String>> addresses();

    /**
     * The port that the listener should listen on
     *
     * @return the port value for the listener binding
     */
    @WithDefault("53")
    Optional<Integer> port();

    @Override
    default Diff diff(Listener other) {
        final Set<String> diffSet = new HashSet<>();
        if (other == null) {
            diffSet.add("");
            diffSet.add("name");
            diffSet.add("type");
            diffSet.add("addresses");
            diffSet.add("port");
        } else {
            if(!Objects.equals(this.name(), other.name())) {
                diffSet.add("name");
            }
            if (!Objects.equals(this.type(), other.type())) {
                diffSet.add("type");
            }
            diffSet.addAll(StringDiffable.compare("addresses", this.addresses(), other.addresses()).differences());
            if (this.port().isPresent() && other.port().isPresent() && (!this.port().get().equals(other.port().get()))) {
                diffSet.add("port");
            } else if ((this.port().isPresent() && other.port().isEmpty()) || (this.port().isEmpty() && other.port().isPresent())) {
                diffSet.add("port");
            }
            if (!diffSet.isEmpty()) {
                diffSet.add("listener");
            }
        }
        return new Diff("listener", diffSet);
    }


}
