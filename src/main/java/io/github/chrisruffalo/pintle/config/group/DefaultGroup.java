package io.github.chrisruffalo.pintle.config.group;

import io.github.chrisruffalo.pintle.config.Group;
import io.github.chrisruffalo.pintle.config.Matcher;
import io.github.chrisruffalo.pintle.model.QueryContext;

import java.util.List;
import java.util.Optional;

public class DefaultGroup implements Group {

    public static final Group DEFAULT = new DefaultGroup();

    private DefaultGroup(){

    }

    @Override
    public Optional<List<String>> resolvers() {
        return Optional.empty();
    }

    @Override
    public Optional<List<String>> lists() {
        return Optional.empty();
    }

    @Override
    public Optional<List<Matcher>> matchers() {
        return Optional.empty();
    }

    @Override
    public boolean matches(QueryContext context) {
        return true;
    }

    @Override
    public String name() {
        return null;
    }
}
