package io.github.chrisruffalo.pintle.config;

import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

import java.nio.file.Path;

public interface Etc {

    @WithName("data-dir")
    @WithDefault("./.pintle")
    Path dataDirectory();

}
