package io.github.chrisruffalo.pintle.event;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ListUpdate {
    private String listName;
    private String configId;

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }
}
