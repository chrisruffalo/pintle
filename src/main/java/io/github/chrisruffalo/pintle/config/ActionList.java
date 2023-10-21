package io.github.chrisruffalo.pintle.config;

import io.github.chrisruffalo.pintle.config.diff.Diff;
import io.github.chrisruffalo.pintle.config.diff.Diffable;
import io.smallrye.config.WithDefault;

import java.util.Collections;
import java.util.List;

/**
 * Provides a list of URLS that can map to an action (ALLOW, BLOCK, etc). These lists
 * are provided/created by internet users and usually come in hosts file format or
 * just a list of hostnames. Pintle supports both.
 */
public interface ActionList extends Named, Diffable<ActionList>, NamedComparable<ActionList> {

    /**
     * What action should be taken if the hostname is found in the list
     */
    enum Action {
        /**
         * This basically gives the URL a "pass" and it cannot be
         * blocked by other lists. Allow lists should come first
         * unless a certain blocklist is to take priority.
         */
        ALLOW,

        /**
         * Any URL found on a block list should provide an NXDOMAIN
         * response back to the client. No upstream resolvers
         * should be queried.
         */
        BLOCK
    }

    /**
     * What to interpret each entry/line in the file as
     */
    enum Type {
        /**
         * A flat file with two columns. The first column
         * is the address and the second column is the
         * hostname to resolve to that address.
         */
        HOSTFILE,

        /**
         * A flat file that is a list of REGEX values
         * that should be run against incoming hostnames.
         * This is expensive and should only be run
         * in the event that it is actually necessary.
         */
        REGEX
    }

    /**
     * The type of the list.
     *
     * @see Type
     * @return the user-configured type for this list
     */
    @WithDefault("hostfile")
    Type type();

    /**
     * The action to take when an incoming query
     * matches one of the list items
     *
     * @see Action
     * @return the user-configured action for this list
     */
    @WithDefault("block")
    Action action();

    /**
     * A list of strings describing the sources that
     * should be loaded for this list.
     *
     * @return a list of sources to load
     */
    @WithDefault("")
    List<String> sources();

    @Override
    default Diff diff(ActionList other) {
        return new Diff("list", Collections.emptySet());
    }
}
