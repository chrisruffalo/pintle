package io.github.chrisruffalo.pintle.config;

import io.github.chrisruffalo.pintle.model.ServiceType;

import java.util.Set;

public interface Resolver {

    String name();

    ResolverType type();

    Set<String> sources();

}
