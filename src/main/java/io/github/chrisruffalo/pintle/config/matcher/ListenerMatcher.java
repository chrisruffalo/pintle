package io.github.chrisruffalo.pintle.config.matcher;

import io.github.chrisruffalo.pintle.config.Matcher;
import io.github.chrisruffalo.pintle.config.MatcherType;
import io.github.chrisruffalo.pintle.model.QueryContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListenerMatcher implements Matcher {

    @Override
    public MatcherType type() {
        return MatcherType.LISTENER;
    }

    private Set<String> values = new HashSet<>();

    public Set<String> getValues() {
        return values;
    }

    public void setValues(Set<String> values) {
        this.values = values;
    }

    @Override
    public boolean match(QueryContext against) {
        return values.contains(against.getListenerName());
    }
}
