package io.github.chrisruffalo.pintle.resolution.list;

import io.github.chrisruffalo.pintle.config.ActionList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class SourceHandlerProvider {

    @Inject
    HostFileHandler hostFileHandler;

    public Optional<SourceHandler> get(final ActionList forList) {
        if (forList == null || forList.type() == null) {
            return Optional.empty();
        }

        if (ActionList.Type.HOSTFILE.equals(forList.type())) {
            return Optional.of(hostFileHandler);
        }

        return Optional.empty();
    }

}
