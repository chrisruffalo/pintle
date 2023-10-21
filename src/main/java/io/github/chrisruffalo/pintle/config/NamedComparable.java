package io.github.chrisruffalo.pintle.config;

/**
 * Allows named items to be sorted by their name.
 *
 * @param <T> the type that is named
 */
public interface NamedComparable<T extends Named> extends Named, Comparable<T> {

    @Override
    default int compareTo(T o) {
        if (this.name() == null && o.name() == null) {
            return 0;
        }
        if (o.name() == null) {
            return 1;
        }
        return this.name().compareTo(o.name());
    }
}
