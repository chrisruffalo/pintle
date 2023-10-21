package io.github.chrisruffalo.pintle.config.matcher;

import io.github.chrisruffalo.pintle.config.MatcherType;
import io.github.chrisruffalo.pintle.model.QueryContext;

/**
 * Matches against the listener that is servicing the request.
 */
public class ListenerMatcher extends StringValuesMatcher {

    @Override
    public MatcherType type() {
        return MatcherType.LISTENER;
    }

    @Override
    public boolean match(QueryContext against) {
        return this.getValues().contains(against.getListenerName());
    }
}
