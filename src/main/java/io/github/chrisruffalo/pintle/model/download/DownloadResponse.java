package io.github.chrisruffalo.pintle.model.download;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.ZonedDateTime;

@RegisterForReflection
public class DownloadResponse {

    private boolean downloaded = false;

    private String downloadPath;

    private String etag;

    private String compression;

    private ZonedDateTime cacheUntil;

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    public String getDownloadPath() {
        return downloadPath;
    }

    public void setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public ZonedDateTime getCacheUntil() {
        return cacheUntil;
    }

    public void setCacheUntil(ZonedDateTime cacheUntil) {
        this.cacheUntil = cacheUntil;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public String getCompression() {
        return compression;
    }
}
