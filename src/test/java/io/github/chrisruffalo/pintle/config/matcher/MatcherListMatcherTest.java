package io.github.chrisruffalo.pintle.config.matcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MatcherListMatcherTest {

    @Test
    public void testAndAndOr() {
        final AndMatcher andMatcher = new AndMatcher();
        final OrMatcher orMatcher = new OrMatcher();

        Assertions.assertTrue(andMatcher.diff(orMatcher).changed());
        Assertions.assertTrue(orMatcher.diff(andMatcher).changed());

        Assertions.assertFalse(andMatcher.diff(andMatcher).changed());
        Assertions.assertFalse(orMatcher.diff(orMatcher).changed());
    }

}
