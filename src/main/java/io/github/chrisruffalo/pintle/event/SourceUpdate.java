package io.github.chrisruffalo.pintle.event;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class SourceUpdate {

    private long listId;
    private String listName;
    private String configId;
    private String source;

    public long getListId() {
        return listId;
    }

    public void setListId(long listId) {
        this.listId = listId;
    }

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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
