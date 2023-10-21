package io.github.chrisruffalo.pintle.config.matcher;

import io.github.chrisruffalo.pintle.model.QueryContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

public class SubnetMatcherTest {

    @Test
    public void ipv4() {
        final SubnetMatcher matcher = new SubnetMatcher();
        matcher.getValues().add("192.168.0.0/24");
        matcher.getValues().add("192.168.5.0/24");

        Assertions.assertTrue(matcher.match(new QueryContext("1", new TestResponder("192.168.0.44"), new Message())));
        Assertions.assertFalse(matcher.match(new QueryContext("2", new TestResponder("192.168.1.44"), new Message())));
        Assertions.assertFalse(matcher.match(new QueryContext("3", new TestResponder("192.168.2.44"), new Message())));
        Assertions.assertFalse(matcher.match(new QueryContext("4", new TestResponder("192.168.3.44"), new Message())));
        Assertions.assertFalse(matcher.match(new QueryContext("5", new TestResponder("192.168.4.44"), new Message())));
        Assertions.assertTrue(matcher.match(new QueryContext("6", new TestResponder("192.168.5.44"), new Message())));

        Assertions.assertTrue(matcher.match(new QueryContext("1", new TestResponder("::ffff:c0a8:2c"), new Message())));
        Assertions.assertFalse(matcher.match(new QueryContext("2", new TestResponder("::ffff:c0a8:12c"), new Message())));
        Assertions.assertTrue(matcher.match(new QueryContext("6", new TestResponder("::ffff:c0a8:52c"), new Message())));
    }

    //@Test
    public void ipv6() {
        // todo
    }

}
