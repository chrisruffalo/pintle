package io.github.chrisruffalo.pintle.resolution.list;

import io.github.chrisruffalo.pintle.cmd.line.CommandLineRoot;
import io.github.chrisruffalo.pintle.config.ActionList;
import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.model.download.DownloadResponse;
import io.github.chrisruffalo.pintle.model.list.StoredSource;
import io.github.chrisruffalo.pintle.util.DownloadUtil;
import io.github.chrisruffalo.pintle.util.PathUtil;
import io.github.chrisruffalo.pintle.util.ShaUtil;
import io.quarkus.panache.common.Parameters;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.zip.GZIPInputStream;

/**
 * Handler that can cache/handle local and remote files (http/https)
 */
public abstract class FileSourceHandler implements SourceHandler {

    @Inject
    Logger logger;

    @Inject
    CommandLineRoot commandLineRoot;

    @ConfigProperty(name = "pintle.cache")
    Path cachePath;

    @Override
    @Transactional
    @RunOnVirtualThread
    public Optional<StoredSource> load(long listId, PintleConfig pintleConfig, ActionList config, String source) {
        if (config == null) {
            return Optional.empty();
        }

        URI sourceUri;
        try {
            sourceUri = URI.create(source);
        } catch (Exception ex) {
            logger.errorf("[%s] could not parse '%s' as a valid uri for a list source: %s", config.name(), source, ex.getMessage());
            return Optional.empty();
        }

        // see if the source exists already
        final StoredSource storedSource = (StoredSource) StoredSource.find("uri = :uri", Parameters.with("uri", sourceUri.toString())).firstResultOptional().orElseGet(() -> {
            final StoredSource newSource = new StoredSource();
            newSource.uri = sourceUri.toString();
            newSource.persist();
            return newSource;
        });
        String etag = storedSource.etag;
        final String previousDataPath = storedSource.dataPath;

        // here we need to go about looking for the file
        if (sourceUri.getScheme() == null) {
            // attempt to find relative files in the path relative to the configuration
            // and relative to the pintle home
            final Optional<Path> sourcePath = PathUtil.find(
                PathUtil.real(source),
                commandLineRoot.configuration.getParent(),
                pintleConfig.etc().home()
            );
            if (sourcePath.isEmpty()) {
                logger.infof("[%s] could not find the file source '%s'", config.name(), sourceUri);
                return Optional.empty();
            }
        }

        // if the scheme is http/https handle the url

        // otherwise if the scheme is null load it from the file
        if ("http".equalsIgnoreCase(sourceUri.getScheme()) || "https".equalsIgnoreCase(sourceUri.getScheme())) {
            DownloadResponse response;

            // if the file is missing reset the etag
            if (Optional.ofNullable(storedSource.dataPath).isPresent() && !Files.exists(PathUtil.real(storedSource.dataPath))) {
                etag = null;
                storedSource.etag = null;
            }

            try {
                response = DownloadUtil.download(sourceUri, PathUtil.real(cachePath), etag).toCompletableFuture().get();
            } catch (InterruptedException | ExecutionException e) {
                response = new DownloadResponse();
                response.setDownloaded(false);
            }
            storedSource.etag = Optional.ofNullable(response.getEtag()).orElse(storedSource.etag);
            storedSource.dataPath = Optional.ofNullable(response.getDownloadPath()).orElse(storedSource.dataPath);
            storedSource.compression = Optional.ofNullable(response.getCompression()).orElse(storedSource.compression);
            if (response.isDownloaded()) {
                if (Optional.ofNullable(response.getDownloadPath()).isPresent()) {
                    logger.debugf("downloaded %s to %s", sourceUri, response.getDownloadPath());
                } else {
                    logger.debugf("%s cached at %s", sourceUri, storedSource.dataPath);
                }
            }
        }

        // get hashes
        final String newHash = ShaUtil.hash(PathUtil.real(storedSource.dataPath)).orElse("");
        final String newContentHash = ShaUtil.hash(PathUtil.real(storedSource.dataPath), storedSource.compression).orElse("");

        // the file should be considered updated when the file hash has changed or the content hash has changed (and does not match the hash)
        boolean updated = newHash.equalsIgnoreCase(storedSource.contentHash)
                     || (!newContentHash.isEmpty() && !newContentHash.equalsIgnoreCase(newHash) && !newContentHash.equalsIgnoreCase(storedSource.contentHash));

        // if the current data path exists and is not the same as the old path then we
        // need to clean up the old data file if we can.
        if (previousDataPath != null && !previousDataPath.isEmpty() && !previousDataPath.equalsIgnoreCase(storedSource.dataPath)) {
            try {
                Files.deleteIfExists(PathUtil.real(previousDataPath));
            } catch (IOException e) {
                logger.errorf("The source %s has changed but could not delete previous data file %s", source, previousDataPath);
            }
        }

        if (updated) {
            // save the hashes
            storedSource.hash = newHash;
            storedSource.contentHash = newContentHash;

            // it has updated so change the version
            storedSource.version++;
        }

        // ensure source is persisted at the end
        storedSource.persist();

        if (storedSource.id == null) {
           final Optional<StoredSource> optionalStoredSource = StoredSource.findByIdOptional(storedSource.uri);
           if (optionalStoredSource.isPresent()) {
               logger.debugf("found id after search: %d", optionalStoredSource.get().id);
               return optionalStoredSource;
           }
        }

        return Optional.of(storedSource);
    }

    protected BufferedReader open(StoredSource storedSource) throws IOException {
        if ("gzip".equalsIgnoreCase(storedSource.compression)) {
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(PathUtil.real(storedSource.dataPath)))));
        }
        return Files.newBufferedReader(PathUtil.real(storedSource.dataPath));
    }
}
