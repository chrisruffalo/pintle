package io.github.chrisruffalo.pintle.config;

import io.smallrye.config.WithDefault;

import java.util.List;

public interface ActionList {

    enum Action {
        ALLOW,
        BLOCK
    }

    enum ActionType {
        FILE,
        REGEX
    }

    String name();

    @WithDefault("file")
    ActionType type();

    @WithDefault("block")
    Action action();

    @WithDefault("")
    List<String> sources();

}
