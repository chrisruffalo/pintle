package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.producer.ConfigProducer;
import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.event.ConfigUpdate;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.github.chrisruffalo.pintle.model.log.AnswerItem;
import io.github.chrisruffalo.pintle.model.log.LogItem;
import io.github.chrisruffalo.pintle.telemetry.SpanController;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.hibernate.StatelessSession;
import org.jboss.logging.Logger;
import org.xbill.DNS.Message;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@ApplicationScoped
public class LoggingController extends PostRespondController {

    @Inject
    @PersistenceUnit("log-db")
    StatelessSession statelessSession;

    @Inject
    ConfigProducer configProducer;

    PintleConfig pintleConfig;

    @Inject
    Logger logger;

    @Inject
    Tracer tracer;

    @Inject
    SpanController spanController;

    Queue<LogItem> logItemQueue = new ConcurrentLinkedQueue<>();

    public void init(@Observes StartupEvent start){
        // has 2 post-query events
        this.register(2);
    }

    @ConsumeEvent(value = Bus.CONFIG_UPDATE_LOGGING, ordered = true)
    public void configure(ConfigUpdate event) {
        if (!event.isInitial()) {
            logger.debugf("logging subsystem config update %s", event.getId());
        }
        this.pintleConfig = configProducer.get(event.getId());
    }

    @ConsumeEvent(Bus.QUERY_DONE)
    @RunOnVirtualThread
    public void log(QueryContext context) {
        // quick return if not enabled
        if (pintleConfig == null || !pintleConfig.log().enabled() || !pintleConfig.log().stdout()) {
            if (done(context)){
                context.getSpan().end();
            }
            return;
        }

        try (final Scope outer = spanController.startAsCurrent(context)) {
            final Span span = tracer.spanBuilder("log-to-console").startSpan();
            try (final Scope scope = span.makeCurrent()) {
                final Message question = context.getQuestion();
                final Message answer = context.getAnswer();
                String appended = String.format("[%dms]", context.getElapsedMs());
                if (!context.getExceptions().isEmpty()) {
                    logger.errorf("[%s] encountered %d error(s) starting with: %s", context.getTraceId(), context.getExceptions().size(), context.getExceptions().getFirst().getMessage());
                } else if (question != null) {
                    final String log = String.format("answered question id=%s type=%s name=%s %s", question.getHeader().getID(), Type.string(question.getQuestion().getType()), question.getQuestion().getName().toString(false), appended);
                    logger.infof("[%s] %s", context.getTraceId(), log);
                    context.getSpan().addEvent(log);
                } else if (answer != null) {
                    logger.debugf("[%s] responded with answer id=%s %s", context.getTraceId(), answer.getHeader().getID(), appended);
                }
            } finally {
                span.end();
            }
        }

        if (done(context)){
            context.getSpan().end();
        }
    }

    @ConsumeEvent(Bus.QUERY_DONE)
    @RunOnVirtualThread
    public void logToDatabaseCollector(QueryContext context) {
        // quick return if not enabled
        if (pintleConfig == null || !pintleConfig.log().enabled() || !pintleConfig.log().database().enabled()) {
            if (done(context)){
                context.getSpan().end();
            }
            return;
        }

        try (final Scope outer = spanController.startAsCurrent(context)) {
            final Span span = tracer.spanBuilder("log-to-database").startSpan();
            try (final Scope scope = span.makeCurrent()) {

                final LogItem item = new LogItem();
                item.start = context.getStarted();
                item.result = context.getResult();
                item.elapsedTime = context.getElapsedMs();
                item.service = context.getResponder().type();
                item.clientAddress = context.getResponder().toClient();
                Optional.ofNullable(context.getQuestion()).ifPresent(m -> {
                    item.type = m.getQuestion().getType();
                    item.hostname = m.getQuestion().getName().toString(false);
                });
                Optional.ofNullable(context.getAnswer()).ifPresent(m -> {
                    item.responseCode = m.getRcode();
                });
                // skip logging answers if answer logging is disabled
                if (pintleConfig.log().database().answers()) {
                    Optional.ofNullable(context.getAnswer()).ifPresent(m -> {
                        if (m.getSection(Section.ANSWER) != null && !m.getSection(Section.ANSWER).isEmpty()) {
                            m.getSection(Section.ANSWER).stream().filter(Objects::nonNull).forEach(a -> {
                                final AnswerItem answerItem = new AnswerItem();
                                answerItem.logItem = item;
                                answerItem.type = a.getType();
                                answerItem.data = a.rdataToString();
                                item.answers.add(answerItem);
                            });
                        }
                    });
                }
                logItemQueue.add(item);
            } finally {
                span.end();
            }
        }

        if (done(context)){
            context.getSpan().end();
        }
    }

    <T> List<T> drain(Queue<T> source) {
        final List<T> output = new LinkedList<>();
        while (!source.isEmpty()) {
            output.add(source.remove());
        }
        return output;
    }

    @Transactional
    @Scheduled(every = "1s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void flushLogs() {
        if (logItemQueue.isEmpty()) {
            return;
        }

        List<LogItem> toSave = drain(logItemQueue);

        long flushed = 0;
        for(LogItem toFlush : toSave) {
            statelessSession.insert(toFlush);
            flushed++;
        }

        logger.tracef("flushed %d log items", flushed);
    }
}
