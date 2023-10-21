package io.github.chrisruffalo.pintle.config.matcher;

import io.github.chrisruffalo.pintle.model.QueryContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

public class RangeMatcherTest {

    @Test
    public void testDiff() {
        final RangeMatcher rangeOne = new RangeMatcher();
        rangeOne.setStart("100");
        rangeOne.setEnd("200");

        final RangeMatcher rangeTwo = new RangeMatcher();
        rangeTwo.setStart("150");
        rangeTwo.setEnd("200");

        final RangeMatcher rangeThree = new RangeMatcher();
        rangeThree.setStart("100");
        rangeThree.setEnd("150");

        final RangeMatcher rangeFour = new RangeMatcher();
        rangeFour.setStart("99");
        rangeFour.setEnd("201");

        // should be the same in forward and reverse
        Assertions.assertTrue(rangeOne.diff(rangeTwo).changed("start"));
        Assertions.assertFalse(rangeOne.diff(rangeTwo).changed("end"));
        Assertions.assertTrue(rangeTwo.diff(rangeOne).changed("start"));
        Assertions.assertFalse(rangeTwo.diff(rangeOne).changed("end"));

        Assertions.assertFalse(rangeOne.diff(rangeThree).changed("start"));
        Assertions.assertTrue(rangeOne.diff(rangeThree).changed("end"));

        Assertions.assertTrue(rangeOne.diff(rangeFour).changed("start"));
        Assertions.assertTrue(rangeOne.diff(rangeFour).changed("end"));

        // when the right hand compare is null we should see child properties
        Assertions.assertTrue(rangeOne.diff(null).changed("start"));
        Assertions.assertTrue(rangeOne.diff(null).changed("end"));
    }

    @Test
    public void testSimpleMatch() {
        final RangeMatcher rangeMatcher = new RangeMatcher();
        rangeMatcher.setStart("192.168.0.1");
        rangeMatcher.setEnd("192.168.0.10");

        Assertions.assertTrue(rangeMatcher.match(new QueryContext("1", new TestResponder("192.168.0.1"), new Message())));
        Assertions.assertTrue(rangeMatcher.match(new QueryContext("9", new TestResponder("192.168.0.9"), new Message())));
        Assertions.assertTrue(rangeMatcher.match(new QueryContext("10", new TestResponder("192.168.0.10"), new Message())));
        Assertions.assertFalse(rangeMatcher.match(new QueryContext("11", new TestResponder("192.168.0.11"), new Message())));

        Assertions.assertTrue(rangeMatcher.match(new QueryContext("1", new TestResponder("::ffff:c0a8:1"), new Message())));
        Assertions.assertTrue(rangeMatcher.match(new QueryContext("9", new TestResponder("::ffff:c0a8:9"), new Message())));
        Assertions.assertTrue(rangeMatcher.match(new QueryContext("10", new TestResponder("::ffff:c0a8:a"), new Message())));
        Assertions.assertFalse(rangeMatcher.match(new QueryContext("11", new TestResponder("::ffff:c0a8:b"), new Message())));
    }

    @Test
    public void testAcrossSubnetMatch() {
        final RangeMatcher rangeMatcher = new RangeMatcher();
        rangeMatcher.setStart("192.168.1.1");
        rangeMatcher.setEnd("192.168.4.10");

        Assertions.assertFalse(rangeMatcher.match(new QueryContext("0", new TestResponder("192.168.0.1"), new Message())));
        Assertions.assertTrue(rangeMatcher.match(new QueryContext("1", new TestResponder("192.168.1.1"), new Message())));
        Assertions.assertTrue(rangeMatcher.match(new QueryContext("9", new TestResponder("192.168.2.9"), new Message())));
        Assertions.assertTrue(rangeMatcher.match(new QueryContext("10", new TestResponder("192.168.3.10"), new Message())));
        Assertions.assertFalse(rangeMatcher.match(new QueryContext("11", new TestResponder("192.168.4.11"), new Message())));
    }

    @Test
    public void testV6Match1() {
        final RangeMatcher rangeMatcher = new RangeMatcher();
        rangeMatcher.setStart("2001:0db8:85a3::8a2e:0370:7334");
        rangeMatcher.setEnd("2001:0db8:85a3::8a00:ff:ffff");

        Assertions.assertTrue(rangeMatcher.match(new QueryContext("0", new TestResponder("2001:0db8:85a3::8a03:a:b"), new Message())));
        Assertions.assertFalse(rangeMatcher.match(new QueryContext("1", new TestResponder("2002:db8:85a3::8a03:a:b"), new Message())));
    }

    @Test
    public void testV6Match2() {
        final RangeMatcher rangeMatcher = new RangeMatcher();
        rangeMatcher.setStart("2001:0DB8:ABCD:0012:0000:0000:0000:0000");
        rangeMatcher.setEnd("2001:0DB8:ABCD:0012:FFFF:FFFF:FFFF:FFFF");

        Assertions.assertTrue(rangeMatcher.match(new QueryContext("0", new TestResponder("2001:0DB8:ABCD:0012:0000:0600:0000:0000"), new Message())));
        Assertions.assertFalse(rangeMatcher.match(new QueryContext("1", new TestResponder("2002:0DB8:ABCD:0012:0000:0000:0000:0000"), new Message())));
    }

    @Test
    public void testV4toV6Match() {
        final RangeMatcher rangeMatcher = new RangeMatcher();
        rangeMatcher.setStart("::ffff:c0a8:1");
        rangeMatcher.setEnd("::ffff:c0a8:a");

        Assertions.assertTrue(rangeMatcher.match(new QueryContext("1", new TestResponder("::ffff:c0a8:1"), new Message())));
        Assertions.assertTrue(rangeMatcher.match(new QueryContext("9", new TestResponder("::ffff:c0a8:9"), new Message())));
        Assertions.assertTrue(rangeMatcher.match(new QueryContext("10", new TestResponder("::ffff:c0a8:a"), new Message())));
        Assertions.assertFalse(rangeMatcher.match(new QueryContext("11", new TestResponder("::ffff:c0a8:b"), new Message())));

        Assertions.assertTrue(rangeMatcher.match(new QueryContext("1", new TestResponder("192.168.0.1"), new Message())));
        Assertions.assertTrue(rangeMatcher.match(new QueryContext("9", new TestResponder("192.168.0.9"), new Message())));
        Assertions.assertTrue(rangeMatcher.match(new QueryContext("10", new TestResponder("192.168.0.10"), new Message())));
        Assertions.assertFalse(rangeMatcher.match(new QueryContext("11", new TestResponder("192.168.0.11"), new Message())));
    }

}
