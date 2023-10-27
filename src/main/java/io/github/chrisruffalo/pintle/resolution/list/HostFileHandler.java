package io.github.chrisruffalo.pintle.resolution.list;

import io.github.chrisruffalo.pintle.config.ActionList;
import io.github.chrisruffalo.pintle.model.list.StoredLine;
import io.github.chrisruffalo.pintle.model.list.StoredSource;
import io.github.chrisruffalo.pintle.util.NameUtil;
import io.github.chrisruffalo.pintle.util.PathUtil;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

@ApplicationScoped
public class HostFileHandler extends FileSourceHandler {

    @Override
    @Transactional
    @RunOnVirtualThread
    public long process(long listId, ActionList config, StoredSource storedSource) {

        // read lines
        final Path dataPath = PathUtil.real(storedSource.dataPath);

        // not sure how this would happen since we juuuust got this event from the
        // thing that loaded it
        if (!Files.exists(dataPath)) {
            return 0;
        }

        final URI uri;
        try {
            uri = new URI(storedSource.uri);
        } catch (URISyntaxException e) {
            // there should be no literal way to get here, the uri had
            // to survive parsing to get into the db and thus into the
            // storedSource
            throw new RuntimeException(e);
        }

        final String host = uri.getHost();
        final String path = uri.getPath();
        String file;
        if (path.endsWith("/")) {
            file = "/";
        } else {
            file = uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1);
        }

        // start the clock
        final ZonedDateTime start = ZonedDateTime.now();

        logger.debugf("load list %d, source %d, from data path: %s", listId, storedSource.id, dataPath);

        final long versionCount = StoredLine.getCountForListSourceVersion(listId, storedSource.id, storedSource.version);
        if (versionCount > 0) {
            logger.infof("[%s] version %d of %s/.../%s already loaded", config.name(), storedSource.version, host, file);
            return versionCount;
        }

        final Set<String> dontLoadDuplicates = new HashSet<>();

        int loaded = 0;
        try (
            final BufferedReader reader = open(storedSource);
        ) {
            String line;
            while((line = reader.readLine()) != null) {
                // strip comments
                if (line.contains("#")) {
                    line = line.substring(0, line.indexOf("#"));
                }
                // parse
                StringTokenizer tokenizer = new StringTokenizer(line);
                String resolveTo = null;
                if (tokenizer.hasMoreTokens()) {
                    resolveTo = tokenizer.nextToken().trim();
                }
                String hostname;
                if (tokenizer.hasMoreTokens()) {
                    hostname = tokenizer.nextToken().trim();
                } else {
                    hostname = null;
                }
                if (hostname == null || hostname.isEmpty()) {
                    continue;
                }
                hostname = NameUtil.string(hostname);

                final String key = hostname + "|" + resolveTo;
                if (dontLoadDuplicates.contains(key)) {
                    continue;
                }
                dontLoadDuplicates.add(key);

                final StoredLine newLine = new StoredLine();
                newLine.listId = listId;
                newLine.sourceId = storedSource.id;
                newLine.hostname = hostname;
                if (resolveTo != null && !resolveTo.isEmpty()) {
                    newLine.resolveTo = resolveTo;
                }
                newLine.version = storedSource.version;
                newLine.persist();
                loaded++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.infof("[%s] %d new from %s/.../%s [%dms]", config.name(), loaded, host, file, start.until(ZonedDateTime.now(), ChronoUnit.MILLIS));

        return loaded;
    }
}
