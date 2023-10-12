package io.github.chrisruffalo.pintle.config;

import io.github.chrisruffalo.pintle.config.matcher.MatcherConverter;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.smallrye.config.WithConverter;

import java.util.List;
import java.util.Optional;

public interface Group {

    String name();

    Optional<List<String>> resolvers();

    Optional<List<String>> lists();

    @WithConverter(MatcherConverter.class)
    Optional<List<Matcher>> matchers();

    default boolean matches(QueryContext context) {
        if(matchers().isPresent() && !matchers().get().isEmpty()) {
            for(final Matcher matcher : matchers().get()) {
                if(matcher.match(context)) {
                    return true;
                }
            }
        }
        return false;
    }

}
