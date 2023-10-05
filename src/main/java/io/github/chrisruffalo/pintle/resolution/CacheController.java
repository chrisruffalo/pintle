package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.resolution.dto.QueryContext;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheImpl;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.eventbus.EventBus;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Type;

import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;

// todo: need to walk down TTL values
public class CacheController {

    @Inject
    Logger logger;

    @Inject
    EventBus bus;

    @Inject
    @CacheName("dns-entries")
    Cache cache;

    @ConsumeEvent(Bus.CHECK_CACHE)
    public Uni<Void> check(QueryContext context) throws UnknownHostException {
        final String key = key(context.getQuestion());
        if (key == null) {
            bus.send(Bus.QUERY, context);
        }
        return cache.get(key, loader -> {
            return null;
        }).flatMap(answer -> {
            if(answer instanceof final Message answerMessage) {
                logger.debugf("got cached key: %s", key);
                context.setAnswer(answerMessage.clone());
                context.getAnswer().getHeader().setID(context.getQuestion().getHeader().getID());
                context.setCached(true);
                bus.send(Bus.RESPOND, context);
            } else {
                bus.send(Bus.QUERY, context);
            }
            return Uni.createFrom().voidItem();
        });
    }

    @ConsumeEvent(Bus.UPDATE_CACHE)
    public void update(QueryContext context) throws UnknownHostException {
        // a context that came from the cache should not update the cache
        if (context.isCached()) {
            return;
        }
        // don't update cache with bad status
        if (context.getQuestion() == null || context.getAnswer() == null || Rcode.NOERROR != context.getAnswer().getRcode()) {
            return;
        }
        // get the key
        final String key = key(context.getQuestion());
        if (key == null) {
            return;
        }
        logger.debugf("caching key: %s", key);
        cache.as(CaffeineCache.class).put(key, CompletableFuture.completedFuture(context.getAnswer()));
    }

    private String key(Message question) {
        if (question == null) {
            return null;
        }
        return String.format("%s:%s", Type.string(question.getQuestion().getType()), question.getQuestion().getName().toString(true));
    }



}
