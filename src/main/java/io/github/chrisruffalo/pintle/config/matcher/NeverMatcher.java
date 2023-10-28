package io.github.chrisruffalo.pintle.config.matcher;

import io.github.chrisruffalo.pintle.config.Matcher;
import io.github.chrisruffalo.pintle.config.MatcherType;
import io.github.chrisruffalo.pintle.config.diff.Diff;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Collections;
import java.util.Set;

/**
 * Implementation of a matcher that never matches.
 */
@RegisterForReflection
public class NeverMatcher extends BaseMatcher {

    @Override
    public MatcherType type() {
        return MatcherType.NONE;
    }

    @Override
    protected Set<String> allProperties() {
        return Collections.emptySet();
    }

    @Override
    protected Diff internalDiff(Matcher other) {
        return new Diff("neverMatcher", Collections.singleton(""));
    }
}
