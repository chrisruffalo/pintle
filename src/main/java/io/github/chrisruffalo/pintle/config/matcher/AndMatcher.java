package io.github.chrisruffalo.pintle.config.matcher;

import io.github.chrisruffalo.pintle.config.Matcher;
import io.github.chrisruffalo.pintle.config.MatcherType;
import io.github.chrisruffalo.pintle.model.QueryContext;

/**
 * A matcher that requires all children matchers
 * to match for it to return a true match.
 */
public class AndMatcher extends MatcherListMatcher {

    @Override
    public MatcherType type() {
        return MatcherType.AND;
    }

    @Override
    public boolean match(QueryContext against) {
        for(final Matcher matcher : this.getMatchers()) {
            if (!matcher.match(against)) {
                return false;
            }
        }
        return true;
    }
}
