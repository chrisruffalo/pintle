package io.github.chrisruffalo.pintle.config.diff;

/**
 * Interface that enables a class to participate
 * in finding differences between itself and
 * another object of the same class.
 *
 * @param <T> self-type to diff against
 */
public interface Diffable<T> {

    /**
     * Create a diff of this object
     * and all child objects as necessary.
     * If the other object is null
     * all child properties should
     * be included in the diff set.
     *
     * @param other object instance to diff against
     * @return an object representing the differences between that object and this one
     */
    Diff diff(T other);

    /**
     * Accessor method that allows
     * access to the self-object as
     * an unwrapped instance of
     * the referenced class
     *
     * @return usually this;
     */
    @SuppressWarnings("unchecked")
    default T unwrap() {
        return (T)this;
    }

}
