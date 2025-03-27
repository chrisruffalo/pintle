package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.github.chrisruffalo.pintle.model.QueryResult;
import io.github.chrisruffalo.pintle.resolution.responder.Responder;
import io.github.chrisruffalo.pintle.telemetry.SpanController;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

public class CacheController extends PostRespondController {

    @Inject
    Logger logger;

    @Inject
    EventBus bus;

    @Inject
    @CacheName("dns-entries")
    Cache cache;

    @Inject
    Tracer tracer;

    @Inject
    SpanController spanController;

    public void init(@Observes StartupEvent start){
        // has 1 post-query event
        this.register(1);
    }

    /**
     * Caches an answer with the time it was cached
     * so that we can adjust records coming out
     * of the cache and adjust the TTL
     */
    private static class CachedAnswer {

        private final Message answer;

        private final ZonedDateTime cachedAt;

        CachedAnswer(Message answer) {
            this.answer = answer;
            cachedAt = ZonedDateTime.now();
        }

        private Message adjust(int messageId) {
            final Message adjusted = answer.clone();
            adjusted.getHeader().setID(messageId);
            adjusted.removeAllRecords(Section.ANSWER);
            final ZonedDateTime now = ZonedDateTime.now();
            final long secondsElapsed = now.toEpochSecond() - cachedAt.toEpochSecond();

            for(Record record : answer.getSection(Section.ANSWER)) {
                long ttl = record.getTTL() - secondsElapsed;
                if (ttl <= 0) {
                    continue;
                }
                final Record adjustedRecord = Record.newRecord(record.getName(), record.getType(), record.getDClass(), ttl, record.rdataToWireCanonical());
                adjusted.addRecord(adjustedRecord, Section.ANSWER);
            }

            return adjusted;
        }
    }

    @ConsumeEvent(Bus.CHECK_CACHE)
    public Uni<Void> check(QueryContext context) throws UnknownHostException {
        try (final Scope outer = spanController.startAsCurrent(context)) {
            final Span span = tracer.spanBuilder("check-cache").startSpan();
            try (final Scope scope = span.makeCurrent()) {

                final String key = key(context.getResponder(), context.getQuestion());
                if (key == null) {
                    span.setAttribute("pintle.was-cached", false);
                    bus.send(Bus.QUERY, context);
                    span.end();
                    return Uni.createFrom().voidItem();
                }

                // return an item from the cache and if nothing is found
                // return null so that we can spawn a query.
                return cache.get(key, loader -> null).flatMap(cachedItem -> {
                    if (cachedItem instanceof final CachedAnswer cachedAnswer) {
                        span.setAttribute("pintle.was-cached", true);
                        logger.debugf("got cached key: %s", key);
                        final int id = context.getQuestion().getHeader().getID();

                        // create an adjusted answer meaning all the ttl values
                        // have been adjusted down.
                        final Message answer = cachedAnswer.adjust(id);

                        // if there are no answers left then forward to the QUERY bus
                        // for resolution
                        if (answer.getSection(Section.ANSWER).isEmpty()) {
                            logger.debugf("cached entry for key %s has expired (no records with ttl left)", key);
                            bus.send(Bus.QUERY, context);
                            return Uni.createFrom().voidItem();
                        }

                        // if the answer section has content mark it as cached and go
                        // directly to the responder (it is marked as cached so after
                        // the response it won't update the cache)
                        context.setAnswer(answer);
                        context.setResult(QueryResult.CACHED);
                        context.setCached(true);
                        bus.send(Bus.RESPOND, context);
                    } else {
                        bus.send(Bus.QUERY, context);
                    }
                    span.end();
                    return Uni.createFrom().voidItem();
                });
            }
        }
    }

    @ConsumeEvent(Bus.QUERY_DONE)
    @RunOnVirtualThread
    public void update(QueryContext context) throws UnknownHostException {
        try (final Scope outer = spanController.startAsCurrent(context)) {
            final Span span = tracer.spanBuilder("update-cache").setAttribute("pintle.cache-updated", true).startSpan();
            try (final Scope scope = span.makeCurrent()) {
                // a context that came from the cache should not update the cache
                if (context.isCached()) {
                    if (done(context)){
                        context.getSpan().end();
                    }
                    return;
                }
                // don't update cache with bad status
                if (context.getQuestion() == null || context.getAnswer() == null || Rcode.NOERROR != context.getAnswer().getRcode()) {
                    span.end();
                    if (done(context)){
                        context.getSpan().end();
                    }
                    return;
                }
                // if there are errors keep moving
                if (!context.getExceptions().isEmpty()) {
                    span.end();
                    if (done(context)){
                        context.getSpan().end();
                    }
                    return;
                }

                // get the key
                final String key = key(context.getResponder(), context.getQuestion());
                if (key == null) {
                    span.end();
                    if (done(context)){
                        context.getSpan().end();
                    }
                    return;
                }
                logger.debugf("caching key: %s", key);
                cache.as(CaffeineCache.class).put(key, CompletableFuture.completedFuture(new CachedAnswer(context.getAnswer().clone())));
                Span.current().setAttribute("pintle.cache-key", key);
                Span.current().setAttribute("pintle.cache-updated", true);
                span.end();
            }
        }

        if (done(context)){
            context.getSpan().end();
        }
    }

    private String key(Responder responder, Message question) {
        if (question == null) {
            return null;
        }
        return String.format("%s:%s:%s", responder.type(), Type.string(question.getQuestion().getType()), question.getQuestion().getName().toString(false));
    }

    public long count() {
        return cache.as(CaffeineCache.class).keySet().size();
    }

}
