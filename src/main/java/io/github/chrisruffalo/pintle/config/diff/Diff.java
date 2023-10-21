package io.github.chrisruffalo.pintle.config.diff;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This is a simple container that tells us what has
 * changed between two objects. Two diffable objects
 * self-comprare and return this container. This
 * container can be inspected to see if changes
 * in the object tree need to be responded to.
 */
public class Diff {

    private static final String SEP = ".";

    final String name;

    final Set<String> differences;

    public Diff(Set<String> diffSet) {
        this("", diffSet);
    }

    public Diff(String name, Set<String> diffSet) {
        this.name = name;
        this.differences = diffSet != null ? diffSet : Collections.emptySet();
    }

    public boolean changed() {
        return !this.differences.isEmpty();
    }

    public boolean changed(final String property) {
        return this.differences.contains(property);
    }

    public Set<String> differences() {
        return differences(false);
    }

    public Set<String> differences(boolean useLocalName) {
        return differences.stream().sorted().map(s -> {
            if (useLocalName && name != null && !name.isEmpty()) {
                if (s == null || s.isEmpty()) {
                    return name;
                }
                return name + SEP + s;
            }
            return s;
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> differences(final String prefix) {
        return differences(prefix, false);
    }

    public Set<String> differences(final String prefix, boolean useLocalName) {
        return differences(useLocalName).stream().map(d -> {
            if (d == null || d.isEmpty()) {
                return prefix;
            }
            return prefix + SEP + d;
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static <T extends Diffable<T>> Diff compare(T left, T right) {
        if (left == null && right == null) {
            return new Diff(null);
        } else if (left != null) {
            return left.diff(right);
        } else {
            return right.diff(null);
        }
    }

    /**
     * Given a list of strings this compares the left-hand list
     * and right-hand list after sorting them.
     *
     * @param listName
     * @param left
     * @param right
     * @return
     */
    public static  <T> Diff compare(final String collectionName, Collection<? extends Diffable<T>> left, Collection<? extends Diffable<T>> right) {
        if (left == null && right == null) {
            return new Diff(null);
        }
        final Set<String> diffSet = new HashSet<>();
        if (left == null || right == null) {
            diffSet.add(collectionName);
            return new Diff(null);
        }
        if (left.isEmpty() && right.isEmpty()) {
            return new Diff(null);
        }
        if (left.size() != right.size()) {
            diffSet.add(collectionName);
        }
        final Iterator<? extends Diffable<T>> leftIterator = left.iterator();
        final Iterator<? extends Diffable<T>> rightIterator = right.iterator();
        IntStream.range(0, left.size())
            .forEach(index -> {
                final String marker = collectionName + "[" + index + "]";
                if (!rightIterator.hasNext()) {
                    diffSet.add(marker);
                    return;
                }
                Diffable<T> leftValue = leftIterator.next();
                Diffable<T> rightValue = rightIterator.next();
                if (leftValue == null && rightValue == null) {
                    return;
                }
                if (leftValue == null || rightValue == null) {
                    diffSet.add(marker);
                    return;
                }
                Diff compare = leftValue.diff(rightValue.unwrap());
                diffSet.addAll(compare.differences(marker));
            });
        if (!diffSet.isEmpty()) {
            diffSet.add(collectionName);
        }
        return new Diff(diffSet);
    }

    public static <T> Diff compare(final String collectionName, Optional<? extends Collection<? extends Diffable<T>>> left, Optional<? extends Collection<? extends Diffable<T>>> right) {
        if (left.isEmpty() && right.isEmpty()) {
            return new Diff(collectionName, Collections.emptySet());
        }
        if ((left.isPresent() && right.isEmpty()) || (left.isEmpty() && right.isPresent())) {
            return new Diff(collectionName, Collections.singleton(""));
        }
        return compare(collectionName, left.get(), right.get());
    }
}
