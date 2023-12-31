package io.github.chrisruffalo.pintle.event;

/**
 * A list of the various event buses in the application
 * so that they can be used without copy-paste.
 */
public class Bus {

    /**
     * The bus that is used to resolve a query
     */
    public static final String QUERY = "pintle-query";

    /**
     * This is a broadcast topic that is fired when the
     * query is complete
     */
    public static final String QUERY_DONE = "pintle-query-done";

    /**
     * The bus that causes a response to be sent through a responder
     */
    public static final String RESPOND = "pintle-respond";

    /**
     * Where a query context goes when there has been an error
     */
    public static final String HANDLE_ERROR = "pintle-error";

    /**
     * Handles the cache lookup of a query
     */
    public static final String CHECK_CACHE = "pintle-check-cache";

    /**
     * When a query is resolved this is the bus that stores it in a cache
     */
    public static final String UPDATE_CACHE = "pintle-update-cache";

    /**
     * Stores/caches MDNS entries
     */
    public static final String STORE_MDNS = "pintle-store-mdns";

    /**
     * Assigns incoming queries to a group for further processing
     */
    public static final String ASSIGN_GROUP = "pintle-assign-group";

    public static final String HANDLE_ACTION_LISTS = "pitnle-handle-action-lists";

    /**
     * Configuration updates
     */
    public static final String CONFIG_UPDATE_LOGGING = "pintle-config-update-logging";
    public static final String CONFIG_UPDATE_MDNS = "pintle-config-update-mdns";
    public static final String CONFIG_UPDATE_GROUPS = "pintle-config-update-groups";
    public static final String CONFIG_UPDATE_LISTENERS = "pintle-config-update-listeners";
    public static final String CONFIG_UPDATE_LISTS = "pintle-config-update-lists";
    public static final String CONFIG_UPDATE_SINGLE_LIST = "pintle-config-update-single-list";
    public static final String CONFIG_LIST_UPDATE_SOURCE = "pintle-config-update-source";
    public static final String CONFIG_LIST_PROCESS_SOURCE = "pintle-config-process-source";
    public static final String CONFIG_UPDATE_RESOLVERS = "pintle-config-update-resolvers";
    public static final String CONFIG_SINGLE_LIST_COMPLETE = "pintle-config-complete-single-list";
}

