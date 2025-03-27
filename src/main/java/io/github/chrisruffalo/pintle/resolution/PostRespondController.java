package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.model.QueryContext;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class PostRespondController {

    @Inject
    Logger logger;

    private static final Map<String, AtomicInteger> COUNTDOWN_MAP = new ConcurrentHashMap<>();

    private static final AtomicInteger COUNT = new AtomicInteger();

    public void register(int eventCount){
        final int counted = COUNT.addAndGet(eventCount);
        logger.tracef("registered %d post-respond controllers", counted);
    }

    protected synchronized boolean done(QueryContext context) {
        final String traceId = context.getTraceId();
        AtomicInteger remaining = COUNTDOWN_MAP.computeIfAbsent(traceId, (key) -> new AtomicInteger(COUNT.intValue()));
        final int remainingCount = remaining.decrementAndGet();
        logger.tracef("[%s] waiting on %d", traceId, remainingCount);
        if (remainingCount <= 0) {
            COUNTDOWN_MAP.remove(traceId);
        }
        return remainingCount <= 0;
    }
}
