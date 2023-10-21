package io.github.chrisruffalo.pintle.config.diff;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Serves as a way to work with strings and list of strings
 * without being able to modify the base string class.
 */
public class StringDiffable implements Diffable<String> {

    final String value;

    public StringDiffable(String value) {
        this.value = value;
    }

    public String unwrap() {
        return value;
    }

    @Override
    public Diff diff(String other) {
        if (!Objects.equals(this.value, other)) {
            return new Diff(Collections.singleton(""));
        }
        return new Diff(null);
    }

    public static List<StringDiffable> list(Collection<String> from) {
        if(from == null || from.isEmpty()) {
            return Collections.emptyList();
        }
        return from.stream().map(StringDiffable::new).toList();
    }

    public static Set<StringDiffable> set(Collection<String> from) {
        if (from == null || from.isEmpty()) {
            return Collections.emptySet();
        }
        final Set<String> intermediate = new HashSet<>(from);
        return intermediate.stream().map(StringDiffable::new).collect(Collectors.toSet());
    }

    public static Diff compare(String listName, List<String> left, List<String> right) {
        List<? extends Diffable<String>> leftDiffableSorted = list(left);
        List<? extends Diffable<String>> rightDiffableSorted = list(right);
        return Diff.compare(listName, leftDiffableSorted, rightDiffableSorted);
    }

    public static Diff compare(String listName, Set<String> left, Set<String> right) {
        List<? extends Diffable<String>> leftDiffableSorted = list(left);
        List<? extends Diffable<String>> rightDiffableSorted = list(right);
        return Diff.compare(listName, leftDiffableSorted, rightDiffableSorted);
    }

    public static Diff compare(String listName, Optional<? extends Collection<String>> left, Optional<? extends Collection<String>> right) {
        Optional<List<? extends Diffable<String>>> leftDiffableSorted = left.map(StringDiffable::list);
        Optional<List<? extends Diffable<String>>> rightDiffableSorted = right.map(StringDiffable::list);
        return Diff.compare(listName, leftDiffableSorted, rightDiffableSorted);
    }
}
