package io.github.chrisruffalo.pintle.config;

/**
 * A lot of entries in the configuration of
 * pintle can be referenced by name. This is
 * a marker interface that makes collecting
 * them into maps or otherwise identifying
 * them easier.
 *
 */
public interface Named {

    /**
     * The name of the configuration entry
     * usually in a set or list.
     *
     * @return the name of the configuration item
     */
    String name();

}
