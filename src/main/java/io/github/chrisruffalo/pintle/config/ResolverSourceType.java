package io.github.chrisruffalo.pintle.config;

/**
 * The format or mechanism for the resolver source
 * to look up query resolution information.
 */
public enum ResolverSourceType {

    UDP,

    TCP,

    HTTP,

    HTTPS,

    TLS,

    ZONE,

    HOSTFILE,

    CONF,

    RESOLVER

    ;

}
