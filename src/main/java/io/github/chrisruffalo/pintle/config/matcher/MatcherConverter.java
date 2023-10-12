package io.github.chrisruffalo.pintle.config.matcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chrisruffalo.pintle.config.Matcher;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.spi.Converter;
import org.yaml.snakeyaml.Yaml;

import java.util.Collections;
import java.util.List;

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
            //throw new RuntimeException(e);
        }
        return Collections.singletonList(new IpMatcher());
    }
}
