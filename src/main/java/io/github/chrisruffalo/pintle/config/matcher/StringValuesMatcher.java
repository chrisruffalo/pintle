package io.github.chrisruffalo.pintle.config.matcher;

import io.github.chrisruffalo.pintle.config.Matcher;
import io.github.chrisruffalo.pintle.config.diff.Diff;
import io.github.chrisruffalo.pintle.config.diff.StringDiffable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * If a matcher uses a list of strings this provides
 * and implementation of the logic needed for differencing
 * lists of strings (values) in the matcher.
 */
public abstract class StringValuesMatcher extends BaseMatcher {

    private Set<String> values = new LinkedHashSet<>();

    public Set<String> getValues() {
        return values;
    }

    public void setValues(Set<String> values) {
        this.values = values;
    }

    @Override
    protected Set<String> allProperties() {
        return Collections.singleton("values");
    }

    protected Diff internalDiff(Matcher other) {
        if(other == null || !this.getClass().isAssignableFrom(other.getClass()) || !(other instanceof final StringValuesMatcher stringValuesMatcher)) {
            return new Diff(this.getClass().getSimpleName().substring(0, 1).toLowerCase() + this.getClass().getSimpleName().substring(1), Collections.singleton("values"));
        }
        return StringDiffable.compare("values", this.values, stringValuesMatcher.getValues());
    }

}
