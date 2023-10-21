package io.github.chrisruffalo.pintle.config.matcher;

import io.github.chrisruffalo.pintle.model.QueryContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

public class IpMatcherTest {

    @Test
    public void diff() {
        final IpMatcher one = new IpMatcher();
        one.getValues().add("192.168.100.1");
        one.getValues().add("192.168.100.2");
        one.getValues().add("192.168.100.3");

        final IpMatcher two = new IpMatcher();
        two.getValues().add("192.168.100.1");
        two.getValues().add("192.168.100.2");
        two.getValues().add("192.168.100.3");

        Assertions.assertFalse(one.diff(two).changed());
        Assertions.assertFalse(two.diff(one).changed());

        two.getValues().remove("192.168.100.2");
        Assertions.assertTrue(one.diff(two).changed());
        Assertions.assertTrue(two.diff(one).changed());
    }

    @Test
    public void match() {
        QueryContext queryContext = new QueryContext("1", new TestResponder("192.168.1.44"), new Message());

        final IpMatcher matcher = new IpMatcher();
        matcher.getValues().add("192.168.1.44");
        Assertions.assertTrue(matcher.match(queryContext));

        matcher.getValues().clear();
        matcher.getValues().add("192.168.1.45");
        Assertions.assertFalse(matcher.match(queryContext));

        matcher.getValues().clear();
        matcher.getValues().add("::ffff:c0a8:12d");
        Assertions.assertFalse(matcher.match(queryContext));

        matcher.getValues().clear();
        matcher.getValues().add("::ffff:c0a8:12c");
        Assertions.assertTrue(matcher.match(queryContext));
    }


}
