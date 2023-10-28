package io.github.chrisruffalo.pintle.model;

/**
 * What type of listener was used
 * to receive the query
 */
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
