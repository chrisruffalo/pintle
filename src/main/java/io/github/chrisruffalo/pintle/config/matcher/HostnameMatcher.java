package io.github.chrisruffalo.pintle.config.matcher;

import io.github.chrisruffalo.pintle.config.MatcherType;
import io.github.chrisruffalo.pintle.model.QueryContext;

import java.util.Objects;

public class HostnameMatcher extends StringValuesMatcher {

    @Override
    public MatcherType type() {
        return MatcherType.HOSTNAME;
    }

    @Override
    public boolean match(QueryContext against) {
        String hostname = against.getQuestion().getQuestion().getName().toString(false);
        if (hostname == null || hostname.isEmpty()) {
            return false;
        }
        if (!hostname.endsWith(".")) {
            hostname = hostname.trim() + ".";
        }
        hostname = hostname.toLowerCase();
        return getValues().stream().filter(Objects::nonNull).map(String::toLowerCase).map(String::trim).map(s -> {
            if (!s.endsWith(".")) {
                return s + ".";
            }
            return s;
        }).anyMatch(hostname::endsWith);
    }
}
