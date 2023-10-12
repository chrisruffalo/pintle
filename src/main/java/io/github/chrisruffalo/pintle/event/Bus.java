package io.github.chrisruffalo.pintle.event;

public class Bus {

    public static final String QUERY = "pintle-query";

    public static final String RESPOND = "pintle-respond";

    public static final String HANDLE_ERROR = "pintle-error";

    public static final String LOG = "pintle-log";

    public static final String PERSIST_LOG = "pintle-persist-log";

    public static final String CHECK_CACHE = "pintle-check-cache";

    public static final String UPDATE_CACHE = "pintle-update-cache";

    public static final String UPDATE_QUESTION_STATS = "pintle-update-question-stats";

    public static final String UPDATE_CLIENT_STATS = "pintle-update-client-stats";

    public static final String STORE_MDNS = "pintle-store-mdns";

    public static final String ASSIGN_GROUP = "pintle-assign-group";
}
