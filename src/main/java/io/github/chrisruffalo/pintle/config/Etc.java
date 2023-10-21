package io.github.chrisruffalo.pintle.config;

import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.nio.file.Path;

public interface Etc {

    @WithName("home")
    @WithDefault("./.pintle")
    Path home();

    @WithName("reload-on-change")
    @WithDefault("false")
    boolean reload();

}
