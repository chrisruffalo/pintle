package io.github.chrisruffalo.pintle.config.diff;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

public class DiffTest {

    public static class TestEntity implements Diffable<TestEntity> {
        private int id;

        private String prop;

        private TestEntity single;

        private List<TestEntity> children = new LinkedList<>();

        private List<String> values = new LinkedList<>();

        private Set<String> options = new HashSet<>();

        @Override
        public Diff diff(TestEntity other) {
            final Set<String> diffSet = new HashSet<>();
            if(other == null) {
                diffSet.add("");
            } else {
                if (!Objects.equals(id, other.id)) {
                    diffSet.add("id");
                }
                if (!Objects.equals(prop, other.prop)) {
                    diffSet.add("prop");
                }
                if (this.single != null) {
                    diffSet.addAll(single.diff(other.single).differences("single"));
                } else if (other.single != null) {
                    diffSet.add("single");
                }
                if (this.children != null) {
                    diffSet.addAll(Diff.compare("children", this.children, other.children).differences());
                } else if (other.children != null) {
                    diffSet.add("children");
                }
            }
            return new Diff("testEntity", diffSet);
        }
    }

    @Test
    public void testIdenticalEntities() {
        final TestEntity left = new TestEntity();
        left.id = 1;
        left.prop = "prop";

        final TestEntity right = new TestEntity();
        right.id = 1;
        right.prop = "prop";

        Assertions.assertFalse(left.diff(right).changed());
        Assertions.assertFalse(right.diff(left).changed());
    }

    @Test
    public void testDifferentPropertyEntities() {
        final TestEntity left = new TestEntity();
        left.id = 1;
        left.prop = "left";

        final TestEntity right = new TestEntity();
        right.id = 2;
        right.prop = "right";

        final Diff diff = left.diff(right);
        Assertions.assertTrue(diff.changed());
        Assertions.assertTrue(diff.differences().contains("id"));
        Assertions.assertTrue(diff.differences().contains("prop"));
        Assertions.assertFalse(diff.differences().contains("children"));
    }

    @Test
    public void testDifferentSingle() {
        final TestEntity left = new TestEntity();
        left.single = new TestEntity();
        left.single.id = 1;
        left.single.prop = "one";

        final TestEntity right = new TestEntity();
        right.single = new TestEntity();
        right.single.id = 2;
        right.single.prop = "two";

        final Diff diff = left.diff(right);
        Assertions.assertTrue(diff.changed());
        Assertions.assertTrue(diff.differences().contains("single.id"));
        Assertions.assertTrue(diff.differences().contains("single.prop"));
    }

    @Test
    public void stringListNoDifferences() {
        final List<String> left = new LinkedList<>();
        left.add("one");
        left.add("two");
        left.add("three");

        final List<String> right = new LinkedList<>();
        right.add("one");
        right.add("two");
        right.add("three");

        Assertions.assertFalse(StringDiffable.compare("", left, right).changed());
    }

    @Test
    public void testBothEmtpy() {
        Assertions.assertFalse(Diff.compare("empty", Collections.emptyList(), Collections.emptySet()).changed());
    }

    @Test
    public void testBothNull() {
        Assertions.assertFalse(Diff.compare("empty", (Collection<? extends Diffable<Object>>)null, null).changed());
    }

    @Test
    public void stringListThreeDifferences() {
        final List<String> left = new LinkedList<>();
        left.add("one");
        left.add("two");
        left.add("three");

        final List<String> right = new LinkedList<>();
        right.add("four");
        right.add("five");
        right.add("six");

        final Diff diff = StringDiffable.compare("", left, right);
        Assertions.assertTrue(diff.changed());
        Assertions.assertEquals(4, diff.differences().size());
        Assertions.assertTrue(diff.differences().contains(""));
        Assertions.assertTrue(diff.differences().contains("[0]"));
        Assertions.assertTrue(diff.differences().contains("[1]"));
        Assertions.assertTrue(diff.differences().contains("[2]"));
        Assertions.assertFalse(diff.differences().contains("[3]"));
    }

    @Test
    public void stringListLeftLonger() {
        final List<String> left = new LinkedList<>();
        left.add("one");
        left.add("two");
        left.add("three");
        left.add("four");
        left.add("five");

        final List<String> right = new LinkedList<>();
        right.add("one");
        right.add("three");
        right.add("four");

        final Diff diff = StringDiffable.compare("", left, right);
        Assertions.assertTrue(diff.changed());
        Assertions.assertEquals(5, diff.differences().size());
        Assertions.assertTrue(diff.differences().contains(""));
        Assertions.assertTrue(diff.differences().contains("[1]"));
        Assertions.assertTrue(diff.differences().contains("[2]"));
        Assertions.assertTrue(diff.differences().contains("[3]"));
        Assertions.assertTrue(diff.differences().contains("[4]"));
    }

    @Test
    public void stringListRightLonger() {
        final List<String> left = new LinkedList<>();
        left.add("one");
        left.add("two");
        left.add("three");

        final List<String> right = new LinkedList<>();
        right.add("one");
        right.add("two");
        right.add("three");
        right.add("four");
        right.add("five");

        final Diff diff = StringDiffable.compare("", left, right);
        Assertions.assertTrue(diff.changed());
        Assertions.assertEquals(1, diff.differences().size());
        Assertions.assertTrue(diff.differences().contains(""));
    }

}
