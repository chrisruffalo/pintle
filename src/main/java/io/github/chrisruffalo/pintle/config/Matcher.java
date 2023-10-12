package io.github.chrisruffalo.pintle.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.chrisruffalo.pintle.config.matcher.*;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.smallrye.config.WithConverter;

import java.util.List;
import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(value = {
        @JsonSubTypes.Type(value = IpMatcher.class, name = "ip"),
        @JsonSubTypes.Type(value = IpMatcher.class, name = "range"),
        @JsonSubTypes.Type(value = IpMatcher.class, name = "subnet"),
        @JsonSubTypes.Type(value = ListenerMatcher.class, name = "listener"),
        @JsonSubTypes.Type(value = AndMatcher.class, name = "and"),
        @JsonSubTypes.Type(value = OrMatcher.class, name = "or")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@WithConverter(MatcherConverter.class)
public interface Matcher {

    MatcherType type();

    default boolean match(final QueryContext against) {
        return false;
    }

}
