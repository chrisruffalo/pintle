package io.github.chrisruffalo.pintle.config;

/**
 * Enumeration for the types
 * of matchers that are provided
 * in the Pintle configuration.
 *
 */
public enum MatcherType {

    /**
     * No matcher given/configured. Will never match.
     */
    NONE,

    /**
     * Matches against the IP of the client making the query
     */
    IP,

    /**
     * Matches if the client's ip is within a certain IP range
     */
    RANGE,

    /**
     * Matches if the client's ip is within a given subnet
     */
    SUBNET,

    /**
     * Matches if the query came in through a given listener. Can be
     * easier and more reliable than subnet matching.
     */
    LISTENER,

    /**
     * All matchers added as children of this matcher must match
     * the query for this one to match.
     */
    AND,

    /**
     * Any matcher added as a child of this matcher can match
     * and this matcher will match. In other words: if no child
     * matches then this matcher will not match.
     */
    OR,

    /**
     * Matches against the hostname provided in the query. Useful
     * for separating out certain domains for special blocking
     * or resolution.
     */
    HOSTNAME

    ;

}
