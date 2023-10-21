package io.github.chrisruffalo.pintle.config.serde;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chrisruffalo.pintle.config.Matcher;
import io.github.chrisruffalo.pintle.config.matcher.MatcherHolder;
import io.github.chrisruffalo.pintle.config.matcher.NeverMatcher;
import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Converts the matcher interface to the individual matcher fields
 * using JSON subtype differentiation described on the Matcher
 * interface.
 */
public class MatcherConverter implements Converter<List<Matcher>> {

    ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<Matcher> convert(String s) throws IllegalArgumentException, NullPointerException {
        if (s == null || s.isEmpty()) {
            return Collections.emptyList();
        }
        // parse to yaml
        try {
            MatcherHolder holder = mapper.readValue(s, MatcherHolder.class);
            return holder.getMatcherList();
        } catch (JsonProcessingException e) {
            Logger.getLogger(this.getClass()).errorf(e, "could not deserialize string %s", s);
        }
        return Collections.singletonList(new NeverMatcher());
    }
}
