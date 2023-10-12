package io.github.chrisruffalo.pintle.config;

import io.github.chrisruffalo.pintle.model.ServiceType;
import io.smallrye.config.WithDefault;

import java.util.Set;

public interface Resolver {

    String name();

    @WithDefault("false")
    boolean balance();

    @WithDefault("")
    ResolverType type();

    @WithDefault("")
    Set<String> sources();

}
