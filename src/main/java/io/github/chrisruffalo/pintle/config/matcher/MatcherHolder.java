package io.github.chrisruffalo.pintle.config.matcher;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.chrisruffalo.pintle.config.Matcher;

import java.util.LinkedList;
import java.util.List;

/**
 * When the configuration object is parsed it looks
 * like a map with a single value ("matchers") that
 * is a list of the parsed matchers. This object
 * serves to bridge the two easily.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatcherHolder {

    @JsonProperty("matchers")
    private List<Matcher> matcherList = new LinkedList<>();

    public List<Matcher> getMatcherList() {
        return matcherList;
    }

    public void setMatcherList(List<Matcher> matcherList) {
        this.matcherList = matcherList;
    }
}
