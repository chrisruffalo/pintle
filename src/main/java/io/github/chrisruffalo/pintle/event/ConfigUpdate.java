package io.github.chrisruffalo.pintle.event;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * This is a marker update because the configuration cannot be
 * serialized correctly due to Path and Optional elements
 */
@RegisterForReflection
public class ConfigUpdate {

    /**
     * The configuration id, to request it
     * from the producer
     */
    private final String id;

    /**
     * True when this is the initial load
     */
    private final boolean initial;

    public ConfigUpdate(String id, boolean initial) {
        this.id = id;
        this.initial = initial;
    }

    public String getId() {
        return id;
    }

    public boolean isInitial() {
        return initial;
    }
}
