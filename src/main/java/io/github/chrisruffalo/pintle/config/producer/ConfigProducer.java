package io.github.chrisruffalo.pintle.config.producer;

import io.github.chrisruffalo.pintle.cmd.line.Args;
import io.github.chrisruffalo.pintle.cmd.line.CommandLineRoot;
import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.diff.Diff;
import io.github.chrisruffalo.pintle.config.impl.PintleConfigContainer;
import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.event.ConfigUpdate;
import io.github.chrisruffalo.pintle.util.PathUtil;
import io.github.chrisruffalo.pintle.util.ShaUtil;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.yaml.YamlConfigSource;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Overall we should avoid injecting the configuration file
 * using the standard smallrye configuration. That is because if we want
 * to support runtime reloading of the configuration file we need
 * a way to source those changes.
 */
@ApplicationScoped
public class ConfigProducer {

    @Inject
    @Args
    CommandLineRoot root;

    @Inject
    Logger logger;

    @Inject
    EventBus bus;

    final Map<String, PintleConfig> configurations = new HashMap<>();

    boolean watchForChanges = false;

    private final AtomicReference<PintleConfig> config = new AtomicReference<>(null);

    private String hash = "";

    void start(@Observes StartupEvent event) {
        logger.infof("starting configuration provider");
        this.update();
        // before we potentially enable watching for changes
        // we need to ensure the hash is set
        hash = ShaUtil.hash(configPath()).orElse("");
        // this can't really ever change after this. it is
        // loaded here and not in the update() method. it
        // might be ok to add an api method or ux method
        // for updating the config as well.
        watchForChanges = config.get().etc().reload();
    }

    /**
     * Used to get the current active configuration.
     *
     * @return the current (most recently loaded) configuration.
     */
    public PintleConfig current() {
        return config.get();
    }

    /**
     * Get a configuration by id. This allows objects listening
     * for configuration update events to reach back and get
     * the correct configuration for the fired event.
     *
     *
     * @see ConfigUpdate
     * @param id of the configuration.
     * @return the pintle configuration object matching the config update id
     */
    public PintleConfig get(final String id) {
        return configurations.getOrDefault(id, config.get());
    }

    /**
     * Load the configuration and store it, then send out any events
     * that need to be sent to subcomponents listening for configuration
     * change.
     */
    void update() {
        final PintleConfig original = current();
        final Optional<PintleConfig> loaded = load();
        // if nothing was loaded (error) then no updates are needed
        if (loaded.isEmpty()) {
            return;
        }
        // check difference
        final PintleConfig current = loaded.get();
        config.set(current);
        final String id = UUID.randomUUID().toString();
        configurations.put(id, current);

        // using the current as the left side allows us
        // to get changes for the startup load where the
        // original will be null
        final Diff diffNode = Diff.compare(current, original);
        final ConfigUpdate event = new ConfigUpdate(id, configurations.size() == 1, diffNode);

        // send events
        if(diffNode.changed()) {
            // lists will update listeners if necessary
            if (diffNode.changed("lists")) {
                bus.send(Bus.CONFIG_UPDATE_LISTS, event);
            } else if (diffNode.changed("listeners")) {
                bus.send(Bus.CONFIG_UPDATE_LISTENERS, event);
            }
            if (diffNode.changed("mdns")) {
                bus.send(Bus.CONFIG_UPDATE_MDNS, event);
            }
            if (diffNode.changed("groups")) {
                bus.send(Bus.CONFIG_UPDATE_GROUPS, event);
            }
            if (diffNode.changed("resolvers")) {
                bus.send(Bus.CONFIG_UPDATE_RESOLVERS, event);
            }
            if (diffNode.changed("log")) {
                bus.send(Bus.CONFIG_UPDATE_LOGGING, event);
            }
        }
    }

    /**
     * Global way to find out where the configuration file is. Used to resolve
     * relative files.
     *
     * @return the path the configuration file is stored at
     */
    Path configPath() {
        return PathUtil.real(root.configuration);
    }

    /**
     * Loads the configuration from disk.
     *
     * @return an optional holding the configuration if it could be loaded and validated, empty otherwise.
     */
    Optional<PintleConfig> load() {
        final Path pintleConfig = configPath();
        try {
            final PintleConfig loadedConfig = load(pintleConfig);
            // todo: validate after loading
            return Optional.of(loadedConfig);
        } catch (IOException e) {
            logger.errorf("could not re-load configuration from %s", pintleConfig);
        }
        return Optional.empty();
    }

    /**
     * Watches for changes to the configuration file (as configured by the command line option). If
     * a change happens it requests an update() which will cause the configuration to be loaded. If
     * the configuration has meaningfully changed then events will be fired.
     */
    @Blocking
    @Scheduled(every = "5s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void watch() {
        if (!watchForChanges) {
            logger.debugf("not watching for changes");
            return;
        }
        final Path configPath = configPath();
        final Optional<String> digest = ShaUtil.hash(configPath);
        if (digest.isEmpty()) {
            logger.debugf("could not read digest from configuration file, not loading changes");
            return;
        }
        // if there is no change since the last
        // digest then we know we don't need to do
        // anything
        final String currentHash = digest.get();
        if (currentHash.equals(hash)) {
            return;
        }
        logger.debugf("old hash %s, new hash %s", hash, currentHash);
        // load
        final Optional<PintleConfig> watched = load();
        // if we couldn't load the configuration for some reason, bail
        if (watched.isEmpty()) {
            return;
        }
        // check the contents, the changes could be comments
        // or otherwise immaterial
        final PintleConfig checking = watched.get();
        final Diff diff = checking.diff(current());
        // if no changes are recorded then we can bail
        if (!diff.changed()) {
            logger.infof("no configuration differences between current configuration and on-disk configuration");
            return;
        }

        // update hash
        hash = currentHash;
        logger.infof("configuration file %s updated, reloading", configPath);

        // cause reload
        update();
    }

    /**
     * Standard way to load a configuration file given
     * the path to the file.
     *
     * @param pathToConfigFile path to the config file
     * @return a loaded (not validated) instance of the configuration file
     * @throws IOException if the configuration file cannot be loaded
     */
    public static PintleConfig load(final Path pathToConfigFile) throws IOException {
        final SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .withMappingIgnore("pintle.**")
                .withMapping(PintleConfig.class)
                .withSources(new YamlConfigSource(pathToConfigFile.toUri().toURL()))
                ;
        return new PintleConfigContainer(builder.build().getConfigMapping(PintleConfig.class));
    }

}
