package io.github.chrisruffalo.pintle.config;

import io.smallrye.config.WithDefault;

import java.util.Set;

public interface Mdns {

    @WithDefault("true")
    boolean enabled();

    @WithDefault("all")
    Set<String> interfaces();

}
