package io.github.chrisruffalo.pintle.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "pintle")
public interface PintleConfig {

    @WithParentName
    Etc etc();

    Mdns mdns();

    Optional<List<Group>> groups();

    Optional<List<Listener>> listeners();

    Optional<List<ActionList>> lists();

    Optional<List<Resolver>> resolvers();

}
