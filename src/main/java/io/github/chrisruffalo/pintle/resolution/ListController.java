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
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    @ConsumeEvent(value = Bus.CONFIG_UPDATE_LISTS, ordered = true)
    public void configure(ConfigUpdate event) {
        if (!event.isInitial()) {
            logger.debugf("action list subsystem config update %s", event.getId());
        }
        currentConfig = configProducer.get(event.getId());
        listsByName.clear();
        if (currentConfig.lists().isPresent()) {
            for (final ActionList list : currentConfig.lists().get()) {
                listsByName.put(list.name(), list);
                final ListUpdate listUpdate = new ListUpdate();
                listUpdate.setListName(list.name());
                listUpdate.setConfigId(event.getId());
                bus.send(Bus.CONFIG_UPDATE_SINGLE_LIST, listUpdate);
            }
        }
    }

    /**
     * Configure a single list
     *
     * @param listUpdate
     */
    @Blocking
    @ConsumeEvent(value = Bus.CONFIG_UPDATE_SINGLE_LIST, ordered = true)
    @Transactional
    void configSource(ListUpdate listUpdate) {
        final ActionList list = listsByName.get(listUpdate.getListName());
        // not sure how this would happen but we like to be super defensive
        if (list == null) {
            return;
        }
        logger.infof("processing list %s", listUpdate.getListName());
        final StoredList stored = StoredList.byName(listUpdate.getListName()).orElseGet(() -> {
            final StoredList sl = new StoredList();
            sl.name = listUpdate.getListName();
            return sl;
        });
        stored.action = list.action();
        stored.type = list.type();
        stored.lastConfiguration = listUpdate.getConfigId();

        stored.persist();
        list.sources().forEach(source -> {
            final SourceUpdate sourceUpdate = new SourceUpdate();
            sourceUpdate.setSource(source);
            sourceUpdate.setConfigId(listUpdate.getConfigId());
            sourceUpdate.setListName(listUpdate.getListName());
            sourceUpdate.setListId(stored.id);
            bus.send(Bus.CONFIG_LIST_UPDATE_SOURCE, sourceUpdate);
        });
    }


    /**
     * Update/configure a single source item
     *
     * @param sourceUpdate
     */

    @ConsumeEvent(value = Bus.CONFIG_LIST_UPDATE_SOURCE)
    @RunOnVirtualThread
    void configSource(SourceUpdate sourceUpdate) {
        final ActionList list = listsByName.get(sourceUpdate.getListName());
        final PintleConfig pintleConfig = configProducer.get(sourceUpdate.getConfigId());
        final Optional<SourceHandler> processorOptional = sourceHandlerProvider.get(list);
        if (processorOptional.isEmpty()) {
            return;
        }

        final Optional<StoredSource> storedSourceOptional = processorOptional.get().load(sourceUpdate.getListId(), pintleConfig, list, sourceUpdate.getSource());
        if (storedSourceOptional.isEmpty()) {
            return;
        }

        // send for processing
        bus.send(Bus.CONFIG_LIST_PROCESS_SOURCE, new ProcessSource(sourceUpdate.getListId(), list, storedSourceOptional.get()));
    }

    @ConsumeEvent(value = Bus.CONFIG_LIST_PROCESS_SOURCE)
    @RunOnVirtualThread
    void processSource(ProcessSource processSourceEvent) {
        final StoredSource source = processSourceEvent.getSource();
        final Optional<SourceHandler> processorOptional = sourceHandlerProvider.get(processSourceEvent.getConfig());
        if (processorOptional.isEmpty()) {
            // todo: log
            return;
        }
        int loaded = processorOptional.get().process(processSourceEvent.getStoredListId(), processSourceEvent.getConfig(), source);
        logger.debugf("loaded %d items", loaded);
    }

}
