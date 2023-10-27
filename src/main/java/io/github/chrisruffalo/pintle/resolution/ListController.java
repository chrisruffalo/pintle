package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.cmd.line.CommandLineRoot;
import io.github.chrisruffalo.pintle.config.ActionList;
import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.producer.ConfigProducer;
import io.github.chrisruffalo.pintle.event.*;
import io.github.chrisruffalo.pintle.model.list.StoredList;
import io.github.chrisruffalo.pintle.model.list.StoredSource;
import io.github.chrisruffalo.pintle.resolution.list.SourceHandler;
import io.github.chrisruffalo.pintle.resolution.list.SourceHandlerProvider;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.beans.Transient;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class ListController {

    @Inject
    CommandLineRoot commandLineRoot;

    @Inject
    Logger logger;

    @Inject
    ConfigProducer configProducer;

    @Inject
    EventBus bus;

    @Inject
    SourceHandlerProvider sourceHandlerProvider;

    @ConfigProperty(name = "pintle.cache")
    Path cachePath;

    PintleConfig currentConfig;

    private final Map<String, ActionList> listsByName = new HashMap<>();

    private final Map<String, Set<String>> pendingLists = new HashMap<>();

    private final Map<String, ConfigUpdate> cachedUpdate = new HashMap<>();

    private final Map<String, Map<Long, Long>> configIdToSourceVersion = new ConcurrentHashMap<>();

    @ConsumeEvent(value = Bus.CONFIG_UPDATE_LISTS, ordered = true)
    public void configure(ConfigUpdate event) {
        if (!event.isInitial()) {
            logger.debugf("action list subsystem config update %s", event.getId());
        }
        currentConfig = configProducer.get(event.getId());
        listsByName.clear();
        pendingLists.put(event.getId(), new HashSet<>());
        if (event.getDiff().changed("listeners")) {
            this.cachedUpdate.put(event.getId(), event);
        }

        // create new holder in map
        configIdToSourceVersion.put(event.getId(), new ConcurrentHashMap<>());

        if (currentConfig.lists().isPresent()) {
            for (final ActionList list : currentConfig.lists().get()) {
                listsByName.put(list.name(), list);
                final ListUpdate listUpdate = new ListUpdate();
                listUpdate.setListName(list.name());
                listUpdate.setConfigId(event.getId());
                pendingLists.get(event.getId()).add(list.name());
                bus.send(Bus.CONFIG_UPDATE_SINGLE_LIST, listUpdate);
            }
        }
    }

    @Blocking
    @ConsumeEvent(value = Bus.CONFIG_SINGLE_LIST_COMPLETE, ordered = true)
    void listComplete(ListUpdateComplete listUpdateComplete) {
        final String id = listUpdateComplete.getConfigId();
        final String name = listUpdateComplete.getListName();
        logger.debugf("[%s] done processing", listUpdateComplete.getListName());
        final Set<String> awaitingCompletionLists = this.pendingLists.get(id);
        awaitingCompletionLists.remove(name);
        if (awaitingCompletionLists.isEmpty()) {
            logger.infof("all pending lists have completed processing");
            this.pendingLists.remove(id);

            // notify listeners they need to implement the current configuration
            // if this configuration has changes for the listener after the lists
            if (cachedUpdate.containsKey(id)) {
                final ConfigUpdate update = cachedUpdate.remove(id);
                if (update != null) {
                    bus.send(Bus.CONFIG_UPDATE_LISTENERS, update);
                }
            }
        }
    }

    /**
     * Configure a single list
     *
     * @param listUpdate
     */
    @ConsumeEvent(value = Bus.CONFIG_UPDATE_SINGLE_LIST)
    @RunOnVirtualThread
    @Transactional
    boolean configSource(ListUpdate listUpdate) {
        final ActionList list = listsByName.get(listUpdate.getListName());
        // not sure how this would happen but we like to be super defensive
        if (list == null) {
            return false;
        }
        logger.debugf("processing list %s", listUpdate.getListName());
        final StoredList stored = StoredList.byName(listUpdate.getListName()).orElseGet(() -> {
            final StoredList sl = new StoredList();
            sl.name = listUpdate.getListName();
            return sl;
        });
        stored.action = list.action();
        stored.type = list.type();
        stored.lastConfiguration = listUpdate.getConfigId();

        stored.persist();
        final List<Future<Message<Boolean>>> waitingResponses = new ArrayList<>();

        logger.debugf("[%s] starting processing", list.name());
        list.sources().forEach(source -> {
            final SourceUpdate sourceUpdate = new SourceUpdate();
            sourceUpdate.setSource(source);
            sourceUpdate.setConfigId(listUpdate.getConfigId());
            sourceUpdate.setListName(listUpdate.getListName());
            sourceUpdate.setListId(stored.id);
            waitingResponses.add(bus.request(Bus.CONFIG_LIST_UPDATE_SOURCE, sourceUpdate));
        });

        // wait for responses
        waitingResponses.forEach(r -> {
            try {
                r.toCompletionStage().toCompletableFuture().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

        // send event that marks list as complete
        bus.send(Bus.CONFIG_SINGLE_LIST_COMPLETE, new ListUpdateComplete(listUpdate.getConfigId(), list.name()));

        return true;
    }


    /**
     * Update/configure a single source item
     *
     * @param sourceUpdate
     */
    @Transactional
    @ConsumeEvent(value = Bus.CONFIG_LIST_UPDATE_SOURCE)
    @RunOnVirtualThread
    boolean configSource(SourceUpdate sourceUpdate) {
        final ActionList list = listsByName.get(sourceUpdate.getListName());
        final PintleConfig pintleConfig = configProducer.get(sourceUpdate.getConfigId());
        final Optional<SourceHandler> processorOptional = sourceHandlerProvider.get(list);
        if (processorOptional.isEmpty()) {
            return false;
        }

        final Optional<StoredSource> storedSourceOptional = processorOptional.get().load(sourceUpdate.getListId(), pintleConfig, list, sourceUpdate.getSource());
        if (storedSourceOptional.isEmpty()) {
            return false;
        }

        // send for processing
        try {
            final ProcessSource processSource = new ProcessSource(sourceUpdate.getConfigId(), sourceUpdate.getListId(), list, storedSourceOptional.get());
            bus.request(Bus.CONFIG_LIST_PROCESS_SOURCE, processSource)
                    .toCompletionStage().toCompletableFuture().get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @ConsumeEvent(value = Bus.CONFIG_LIST_PROCESS_SOURCE)
    @RunOnVirtualThread
    @Transactional
    boolean processSource(ProcessSource processSourceEvent) {
        final StoredSource source = processSourceEvent.getSource();
        final Optional<SourceHandler> processorOptional = sourceHandlerProvider.get(processSourceEvent.getConfig());
        if (processorOptional.isEmpty()) {
            // todo: log
            return false;
        }

        long loaded = processorOptional.get().process(processSourceEvent.getStoredListId(), processSourceEvent.getConfig(), source);
        logger.debugf("loaded %d items", loaded);

        // add source to version map
        configIdToSourceVersion.get(processSourceEvent.getConfigId()).put(source.id, source.version);

        return true;
    }

    public long getSourceVersion(final String configId, final long sourceId) {
        if (configIdToSourceVersion.containsKey(configId)) {
            if (configIdToSourceVersion.get(configId).containsKey(sourceId)) {
                return configIdToSourceVersion.get(configId).get(sourceId);
            }
        }
        return 0L;
    }

}
