package io.github.chrisruffalo.pintle.config.matcher;

import inet.ipaddr.IPAddress;
import io.github.chrisruffalo.pintle.config.Matcher;
import io.github.chrisruffalo.pintle.config.MatcherType;
import io.github.chrisruffalo.pintle.config.diff.Diff;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.github.chrisruffalo.pintle.util.NetUtil;

import java.util.*;

/**
 * Matches if the client ip is in a given range.
 */
public class RangeMatcher extends BaseMatcher {

    private static final Set<String> PROPS = new HashSet<>() {{
       add("start");
       add("end");
    }};

    private String start;

    private String end;

    @Override
    public MatcherType type() {
        return MatcherType.RANGE;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public boolean match(QueryContext against) {
        // get client ip from query context
        final Optional<IPAddress> clientOptional = NetUtil.fromString(against.getResponder().toClient());
        if (clientOptional.isEmpty()) {
            return false;
        }
        final Optional<IPAddress> startOptional = NetUtil.fromString(this.start);
        final Optional<IPAddress> endOptional = NetUtil.fromString(this.end);
        if (startOptional.isEmpty() || endOptional.isEmpty()) {
            return false;
        }
        IPAddress client = clientOptional.get();
        final IPAddress start = startOptional.get();
        final IPAddress end = endOptional.get();
        if (start.isIPv6() && end.isIPv6() && !client.isIPv6() && client.isIPv6Convertible()) {
            client = client.toIPv6();
        } else if (start.isIPv4() && end.isIPv4() && !client.isIPv4() && client.isIPv4Convertible()) {
            client = client.toIPv4();
        }
        return start.spanWithRange(end).contains(client);
    }

    @Override
    protected Set<String> allProperties() {
        return PROPS;
    }

    @Override
    protected Diff internalDiff(Matcher other) {
        if(!(other instanceof final RangeMatcher rangeMatcher)) {
            return new Diff("rangeMatcher", Collections.singleton(""));
        }
        final Set<String> diffSet = new HashSet<>();
        if(!Objects.equals(this.start, rangeMatcher.start)) {
            diffSet.add("start");
        }
        if(!Objects.equals(this.end, rangeMatcher.end)) {
            diffSet.add("end");
        }
        return new Diff("rangeMatcher", diffSet);
    }
}
