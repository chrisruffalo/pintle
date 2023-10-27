package io.github.chrisruffalo.pintle.config.impl;

import io.github.chrisruffalo.pintle.config.*;
import io.github.chrisruffalo.pintle.config.diff.Diff;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is designed to wrap a
 * configuration class and provide
 * faster or more efficient implementations
 * of the get-by-name methods.
 *
 */
public class PintleConfigContainer implements PintleConfig {

    private final PintleConfig delegate;

    private final Map<String, Group> groupMap;

    private final Map<String, Resolver> resolverMap;

    private final Map<String, Listener> listenerMap;

    private final Map<String, ActionList> actionListMap;

    public PintleConfigContainer(PintleConfig delegate) {
        this.delegate = delegate;
        // attempting to be as efficient as possible with the starting size of the map
        groupMap = new ConcurrentHashMap<>(delegate.groups().map(List::size).orElse(0));
        resolverMap = new ConcurrentHashMap<>(delegate.groups().map(List::size).orElse(0));
        listenerMap = new ConcurrentHashMap<>(delegate.groups().map(List::size).orElse(0));
        actionListMap = new ConcurrentHashMap<>(delegate.groups().map(List::size).orElse(0));
    }

    @Override
    public Etc etc() {
        return delegate.etc();
    }

    @Override
    public Log log() {
        return delegate.log();
    }

    @Override
    public PintleConfig unwrap() {
        return delegate.unwrap();
    }

    @Override
    public Mdns mdns() {
        return delegate.mdns();
    }

    @Override
    public Optional<List<Group>> groups() {
        return delegate.groups();
    }

    @Override
    public Optional<Group> group(String name) {
        return Optional.ofNullable(groupMap.computeIfAbsent(name, k -> delegate.group(k).orElse(null)));
    }

    @Override
    public Optional<List<Listener>> listeners() {
        return delegate.listeners();
    }

    @Override
    public Optional<Listener> listener(String name) {
        return Optional.ofNullable(listenerMap.computeIfAbsent(name, k -> delegate.listener(k).orElse(null)));
    }

    @Override
    public Optional<List<ActionList>> lists() {
        return delegate.lists();
    }

    @Override
    public Optional<ActionList> list(String name) {
        return Optional.ofNullable(actionListMap.computeIfAbsent(name, k -> delegate.list(k).orElse(null)));
    }

    @Override
    public Optional<List<Resolver>> resolvers() {
        return delegate.resolvers();
    }

    @Override
    public Optional<Resolver> resolver(String name) {
        return Optional.ofNullable(resolverMap.computeIfAbsent(name, k -> delegate.resolver(k).orElse(null)));
    }

    @Override
    public Diff diff(PintleConfig other) {
        return delegate.diff(other);
    }
}
