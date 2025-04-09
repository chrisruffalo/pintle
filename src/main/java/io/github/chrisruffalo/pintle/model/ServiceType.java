package io.github.chrisruffalo.pintle.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * What type of listener was used
 * to receive the query
 */
@RegisterForReflection
public enum ServiceType {

    /**
     * A listener using UDP was used (datagram socket server)
     */
    UDP,

    /**
     * A listener using TCP was used (tcp socket server)
     */
    TCP

    ;

}
