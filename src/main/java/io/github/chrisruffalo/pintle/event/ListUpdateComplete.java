package io.github.chrisruffalo.pintle.event;

public class ListUpdateComplete {

    private String configId;

    private String listName;

    public ListUpdateComplete() {

    }

    public ListUpdateComplete(String configId, String listName) {
        this.configId = configId;
        this.listName = listName;
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }
}
