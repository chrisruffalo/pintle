package io.github.chrisruffalo.pintle.config.matcher;

import io.github.chrisruffalo.pintle.config.Matcher;
import io.github.chrisruffalo.pintle.config.MatcherType;
import jakarta.inject.Inject;

import java.util.List;

public class IpMatcher implements Matcher {

    private List<String> values;

    @Override
    public MatcherType type() {
        return MatcherType.IP;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

}
