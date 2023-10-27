package io.github.chrisruffalo.pintle.resolution.action;

public class ActionResult {

    private String configId;

    private long listId;

    private long sourceId;

    private long sourceVersion;

    private String matchContent;

    private String resolveTo;

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public long getListId() {
        return listId;
    }

    public void setListId(long listId) {
        this.listId = listId;
    }

    public long getSourceId() {
        return sourceId;
    }

    public void setSourceId(long sourceId) {
        this.sourceId = sourceId;
    }

    public String getMatchContent() {
        return matchContent;
    }

    public void setMatchContent(String matchContent) {
        this.matchContent = matchContent;
    }

    public String getResolveTo() {
        return resolveTo;
    }

    public void setResolveTo(String resolveTo) {
        this.resolveTo = resolveTo;
    }

    public long getSourceVersion() {
        return sourceVersion;
    }

    public void setSourceVersion(long sourceVersion) {
        this.sourceVersion = sourceVersion;
    }
}
