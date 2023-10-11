package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.github.chrisruffalo.pintle.model.log.AnswerItem;
import io.github.chrisruffalo.pintle.model.log.LogItem;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.xbill.DNS.Message;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class LoggingController {

    @Inject
    Logger logger;

    @Inject
    EventBus bus;

    @WithSpan("log response to stdout")
    @ConsumeEvent(Bus.LOG)
    @RunOnVirtualThread
    public void log(QueryContext context) {
        final Message question = context.getQuestion();
        final Message answer = context.getAnswer();
        String appended = String.format("[%dms]", context.getElapsedMs());
        if (!context.getExceptions().isEmpty()) {
            logger.errorf("[%s] encountered %d error(s) starting with: %s", context.getTraceId(), context.getExceptions().size(), context.getExceptions().getFirst().getMessage());
        } else if (question != null) {
            logger.debugf("[%s] answered question id=%s type=%s name=%s %s", context.getTraceId(), question.getHeader().getID(), Type.string(question.getQuestion().getType()), question.getQuestion().getName().toString(false), appended);
        } else if(answer != null) {
            logger.debugf("[%s] responded with answer id=%s %s", context.getTraceId(), answer.getHeader().getID(), appended);
        }
    }

    @ConsumeEvent(value = Bus.PERSIST_LOG)
    @WithSpan("persist response")
    @Transactional
    @RunOnVirtualThread
    public void logToDatabase(QueryContext context) {
        final LogItem item = new LogItem();
        item.start = context.getStarted();
        item.result = context.getResult();
        item.elapsedTime = context.getElapsedMs();
        item.service = context.getResponder().type();
        item.clientIp = context.getResponder().toClient();
        Optional.ofNullable(context.getQuestion()).ifPresent(m -> {
            item.type = m.getQuestion().getType();
            item.hostname = m.getQuestion().getName().toString(false);
        });
        Optional.ofNullable(context.getAnswer()).ifPresent(m -> {
            item.responseCode = m.getRcode();
        });
        Optional.ofNullable(context.getAnswer()).ifPresent(m -> {
            if (m.getSection(Section.ANSWER) != null && !m.getSection(Section.ANSWER).isEmpty()) {
                m.getSection(Section.ANSWER).forEach(a -> {
                    final AnswerItem answerItem = new AnswerItem();
                    answerItem.logItem = item;
                    answerItem.type = a.getType();
                    answerItem.data = a.rdataToString();
                    item.answers.add(answerItem);
                });
            }
        });
        item.persist();
    }


}
