package io.github.chrisruffalo.pintle.config.matcher;

import io.github.chrisruffalo.pintle.config.Matcher;
import io.github.chrisruffalo.pintle.config.MatcherType;

public class NeverMatcher implements Matcher {

    @Override
    public MatcherType type() {
        return MatcherType.NONE;
    }
}
