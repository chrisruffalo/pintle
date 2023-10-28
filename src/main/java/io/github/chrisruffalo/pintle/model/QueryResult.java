package io.github.chrisruffalo.pintle.model;

/**
 * The overall outcome of a query.
 */
public enum QueryResult {

    /**
     * The query was sent to an upstream resolver
     * source and an answer was returned and sent
     * to the client.
     */
    RESOLVED,

    /**
     * An error was (or errors were) encountered
     * during the resolution process and a response
     * may have been sent to the client. (All errors
     * should end up here regardless of phase. A
     * response may not be sent.)
     */
    ERROR,

    /**
     * The question was received and an acceptable
     * answer was found in the cache. This answer
     * was then sent back to the client.
     */
    CACHED,

    /**
     * The question was matched against a group that
     * contained a list that had an entry that caused
     * the query to be blocked. The answer sent back
     * to the client would typically be NXDOMAIN.
     */
    BLOCKED

    ;

    /**
     * A safe way of parsing the result from a string in
     * cases where a case-insensitive value may be used.
     *
     * @param value representation of the result as a string
     * @return the enumerated value if it exists, otherwise null
     */
    public static QueryResult fromString(final String value) {
        try {
            return QueryResult.valueOf(value.toUpperCase());
        } catch (Exception ex) {
            // no-op
        }
        return null;
    }

}
