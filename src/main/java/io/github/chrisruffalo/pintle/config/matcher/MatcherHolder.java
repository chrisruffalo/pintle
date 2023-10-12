package io.github.chrisruffalo.pintle.config.matcher;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.chrisruffalo.pintle.config.Matcher;
import io.github.chrisruffalo.pintle.config.MatcherType;

import java.util.LinkedList;
import java.util.List;

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
