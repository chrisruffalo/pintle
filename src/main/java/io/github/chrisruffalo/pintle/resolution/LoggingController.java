package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.model.log.LogItem;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.xbill.DNS.Message;
import org.xbill.DNS.Type;

import java.net.UnknownHostException;
import java.util.Optional;

@ApplicationScoped
public class LoggingController {

    @Inject
    Logger logger;

    @Inject
    EventBus bus;

    @WithSpan("log response")
    @ConsumeEvent(Bus.LOG)
    public void log(QueryContext context) throws UnknownHostException {
        final Message question = context.getQuestion();
        final Message answer = context.getAnswer();
        String appended = String.format("[%dms]", context.getElapsedMs());
        if (question != null) {
            logger.infof("[%s] answered question id=%s type=%s name=%s %s", context.getTraceId(), question.getHeader().getID(), Type.string(question.getQuestion().getType()), question.getQuestion().getName().toString(false), appended);
        } else if(answer != null) {
            logger.infof("[%s] responded with answer id=%s %s", context.getTraceId(), answer.getHeader().getID(), appended);
        }
        bus.send(Bus.PERSIST_LOG, context);
    }

    @Blocking
    @ConsumeEvent(Bus.PERSIST_LOG)
    @WithSpan("persist response")
    @Transactional
    public void logToDatabase(QueryContext context) {
        final LogItem item = new LogItem();
        item.trace = context.getTraceId();
        item.start = context.getStarted();
        item.result = context.getResult();
        item.end = context.getResponded();
        item.elapsedTime = context.getElapsedMs();
        Optional.ofNullable(context.getAnswer()).ifPresent(m -> item.answer = m.toWire());
        Optional.ofNullable(context.getQuestion()).ifPresent(m -> item.question = m.toWire());
        item.persist();
    }
}
