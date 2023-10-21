package io.github.chrisruffalo.pintle.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.chrisruffalo.pintle.config.diff.Diffable;
import io.github.chrisruffalo.pintle.config.matcher.*;
import io.github.chrisruffalo.pintle.config.serde.MatcherConverter;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.smallrye.config.WithConverter;

/**
 * A matcher provides configuration for the
 * logic that matches a query to a group.
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(value = {
    @JsonSubTypes.Type(value = IpMatcher.class, name = "ip"),
    @JsonSubTypes.Type(value = IpMatcher.class, name = "range"),
    @JsonSubTypes.Type(value = IpMatcher.class, name = "subnet"),
    @JsonSubTypes.Type(value = ListenerMatcher.class, name = "listener"),
    @JsonSubTypes.Type(value = AndMatcher.class, name = "and"),
    @JsonSubTypes.Type(value = OrMatcher.class, name = "or"),
    @JsonSubTypes.Type(value = HostnameMatcher.class, name = "hostname")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@WithConverter(MatcherConverter.class)
public interface Matcher extends Diffable<Matcher> {

    /**
     * The type of matcher that is being configured
     *
     * @return the matcher type
     */
    MatcherType type();

    /**
     * Each matcher needs to be able to take a QueryContext
     * as a parameter and decide if it matches or does not
     * match that query context.
     *
     * @param against the query context to match against
     * @return true if the query context belongs in this group, false otherwise
     */
    default boolean match(final QueryContext against) {
        return false;
    }

}
