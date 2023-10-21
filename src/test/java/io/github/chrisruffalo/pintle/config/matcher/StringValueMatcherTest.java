package io.github.chrisruffalo.pintle.config.matcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringValueMatcherTest {

    @Test
    public void testSubclassesDontMatch() {
        final IpMatcher ipMatcher = new IpMatcher();
        ipMatcher.getValues().add("1");
        ipMatcher.getValues().add("2");
        ipMatcher.getValues().add("3");

        final ListenerMatcher listenerMatcher = new ListenerMatcher();
        listenerMatcher.getValues().add("1");
        listenerMatcher.getValues().add("2");
        listenerMatcher.getValues().add("3");

        // so at this point the yml for both would be identical but they should have a difference when
        // compared
        Assertions.assertTrue(ipMatcher.diff(listenerMatcher).changed());
        Assertions.assertTrue(listenerMatcher.diff(ipMatcher).changed());

        // but they should not have a difference from themselves
        Assertions.assertFalse(ipMatcher.diff(ipMatcher).changed());
        Assertions.assertFalse(listenerMatcher.diff(listenerMatcher).changed());

    }

}
