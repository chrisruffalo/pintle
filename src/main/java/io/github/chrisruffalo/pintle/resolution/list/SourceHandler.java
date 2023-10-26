package io.github.chrisruffalo.pintle.resolution.list;

import io.github.chrisruffalo.pintle.config.ActionList;
import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.model.list.StoredSource;

import java.util.Optional;

public interface SourceHandler {

    Optional<StoredSource> load(long listId, PintleConfig pintleConfig, ActionList config, String source);

    long process(long listId, ActionList config, StoredSource storedSource);

}
