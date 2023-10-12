package io.github.chrisruffalo.pintle.config.matcher;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.chrisruffalo.pintle.config.Matcher;
import io.github.chrisruffalo.pintle.config.MatcherType;
import io.github.chrisruffalo.pintle.model.QueryContext;

import java.util.LinkedList;
import java.util.List;

public class AndMatcher implements Matcher {

    private List<Matcher> matchers = new LinkedList<>();

    @Override
    public MatcherType type() {
        return MatcherType.AND;
    }

    public List<Matcher> getMatchers() {
        return matchers;
    }

    public void setMatchers(List<Matcher> matchers) {
        this.matchers = matchers;
    }

    @Override
    public boolean match(QueryContext against) {
        for(final Matcher matcher : matchers) {
            if (!matcher.match(against)) {
                return false;
            }
        }
        return true;
    }
}
