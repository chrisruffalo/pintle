package io.github.chrisruffalo.pintle.config;

import io.vertx.core.spi.metrics.TCPMetrics;

public enum ResolverType {

    UDP,

    TCP,

    ZONE,

    FILE,

    RESOLVER

    ;

}
