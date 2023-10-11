package io.github.chrisruffalo.pintle.config;

import io.github.chrisruffalo.pintle.model.ServiceType;
import io.smallrye.config.WithDefault;

import java.net.InetAddress;
import java.util.Optional;
import java.util.Set;

public interface Listener {

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

}
