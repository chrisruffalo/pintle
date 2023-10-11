package io.github.chrisruffalo.pintle.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

import java.util.List;

@ConfigMapping(prefix = "pintle")
public interface PintleConfig {

    @WithParentName
    Etc etc();

    List<Listener> listeners();

    List<Resolver> resolvers();

}
