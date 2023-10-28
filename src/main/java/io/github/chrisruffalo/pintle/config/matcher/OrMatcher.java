package io.github.chrisruffalo.pintle.config.matcher;

import io.github.chrisruffalo.pintle.config.Matcher;
import io.github.chrisruffalo.pintle.config.MatcherType;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * A matcher that mathces if any of it's children
 * match.
 */
@RegisterForReflection
public class OrMatcher extends MatcherListMatcher {

    @Override
    public MatcherType type() {
        return MatcherType.OR;
    }

    @Override
    public boolean match(QueryContext against) {
        for(final Matcher matcher : this.getMatchers()) {
            if (matcher.match(against)) {
                return true;
            }
        }
        return false;
    }
}
