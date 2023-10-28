package io.github.chrisruffalo.pintle.config.matcher;

import io.github.chrisruffalo.pintle.config.Matcher;
import io.github.chrisruffalo.pintle.config.diff.Diff;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Logic for mainly performing diffing when a matcher
 * contains a list of other matchers.
 */
@RegisterForReflection
public abstract class MatcherListMatcher extends BaseMatcher {

    private List<Matcher> matchers = new LinkedList<>();

    public List<Matcher> getMatchers() {
        return matchers;
    }

    public void setMatchers(List<Matcher> matchers) {
        this.matchers = matchers;
    }

    @Override
    protected Set<String> allProperties() {
        return Collections.singleton("values");
    }

    @Override
    protected Diff internalDiff(Matcher other) {
        if(other == null || !this.getClass().isAssignableFrom(other.getClass()) || !(other instanceof final MatcherListMatcher otherMatcher)) {
            return new Diff(this.getClass().getSimpleName().substring(0, 1).toLowerCase() + this.getClass().getSimpleName().substring(1), Collections.singleton("matchers"));
        }
        return Diff.compare("matchers", this.matchers, otherMatcher.getMatchers());
    }

}
