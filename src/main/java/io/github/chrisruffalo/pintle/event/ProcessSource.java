package io.github.chrisruffalo.pintle.event;

import io.github.chrisruffalo.pintle.config.ActionList;
import io.github.chrisruffalo.pintle.model.list.StoredSource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ProcessSource {

    private ActionList config;

    private long storedListId;

    private StoredSource source;

    public ProcessSource() {

    }

    public ProcessSource(final long storedListId, final ActionList config, final StoredSource storedSource) {
        this.storedListId = storedListId;
        this.config = config;
        this.source = storedSource;
    }

    public ActionList getConfig() {
        return config;
    }

    public void setConfig(ActionList config) {
        this.config = config;
    }

    public long getStoredListId() {
        return storedListId;
    }

    public void setStoredListId(long storedListId) {
        this.storedListId = storedListId;
    }

    public StoredSource getSource() {
        return source;
    }

    public void setSource(StoredSource source) {
        this.source = source;
    }
}
