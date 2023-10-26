package io.github.chrisruffalo.pintle.resolution.list;

import io.github.chrisruffalo.pintle.config.ActionList;
import io.github.chrisruffalo.pintle.model.list.StoredLine;
import io.github.chrisruffalo.pintle.model.list.StoredSource;
import io.github.chrisruffalo.pintle.util.PathUtil;
import io.quarkus.hibernate.orm.panache.Panache;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.Session;

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
    public int process(long listId, ActionList config, StoredSource storedSource) {

        // read lines
        final Path dataPath = PathUtil.real(storedSource.dataPath);

        // not sure how this would happen since we juuuust got this event from the
        // thing that loaded it
        if (!Files.exists(dataPath)) {
            return 0;
        }

        // start the clock
        final ZonedDateTime start = ZonedDateTime.now();

        logger.debugf("load list %d, source %d, from data path: %s", listId, storedSource.id, dataPath);

        final Set<String> alreadyLoadedHostnames = StoredLine.getHostnamesByListAndSource(listId, storedSource.id);
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

                final String key = hostname + "|" + resolveTo;
                if (dontLoadDuplicates.contains(key)) {
                    continue;
                }
                dontLoadDuplicates.add(key);

                if (alreadyLoadedHostnames.contains(key)) {
                    // nothing to do here, a line with the same resolution exists
                } else {
                    final StoredLine newLine = new StoredLine();
                    newLine.listId = listId;
                    newLine.sourceId = storedSource.id;
                    newLine.hostname = hostname;
                    if (resolveTo != null && !resolveTo.isEmpty()) {
                        newLine.resolveTo = resolveTo;
                    }
                    newLine.persist();
                    loaded++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // flush current batch/context
        Panache.getEntityManager("list-db").flush();

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

        logger.infof("[%s] %d new from %s/.../%s [%dms]", config.name(), loaded, host, file, start.until(ZonedDateTime.now(), ChronoUnit.MILLIS));

        return loaded;
    }
}
