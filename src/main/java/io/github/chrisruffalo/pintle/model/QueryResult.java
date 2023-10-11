package io.github.chrisruffalo.pintle.model;

public enum QueryResult {

    RESOLVED,

    ERROR,

    CACHED,

    BLOCKED

    ;

    public static QueryResult fromString(final String value) {
        try {
            return QueryResult.valueOf(value.toUpperCase());
        } catch (Exception ex) {
            // no-op
        }
        return null;
    }

}
