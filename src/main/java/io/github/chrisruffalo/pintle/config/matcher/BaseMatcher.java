package io.github.chrisruffalo.pintle.config.matcher;

import io.github.chrisruffalo.pintle.config.Matcher;
import io.github.chrisruffalo.pintle.config.diff.Diff;

import java.util.HashSet;
import java.util.Set;

/**
 * Provides core logic that mainly provides the
 * ability for diff-ing one matcher against another.
 */
public abstract class BaseMatcher implements Matcher {

    protected abstract Diff internalDiff(Matcher other);

    protected abstract Set<String> allProperties();

    @Override
    public Diff diff(Matcher other) {
        final Set<String> diffSet = new HashSet<>();
        if(other == null) {
            diffSet.add("");
            diffSet.add("type");
            diffSet.addAll(allProperties());
        } else {
            if (!this.type().equals(other.type())) {
                diffSet.add("type");
                diffSet.addAll(allProperties());
            } else {
                diffSet.addAll(this.internalDiff(other).differences(false));
            }
        }
        return new Diff("matcher", diffSet);
    }
}
