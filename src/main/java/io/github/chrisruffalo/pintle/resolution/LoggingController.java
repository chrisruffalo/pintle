package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.producer.ConfigProducer;
import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.event.ConfigUpdate;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.github.chrisruffalo.pintle.model.log.AnswerItem;
import io.github.chrisruffalo.pintle.model.log.LogItem;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.UserTransaction;
import org.jboss.logging.Logger;
import org.xbill.DNS.Message;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.util.Optional;

@ApplicationScoped
public class LoggingController {

    @Inject
    ConfigProducer configProducer;

    PintleConfig pintleConfig;

    @Inject
    Logger logger;

    @Inject
    UserTransaction transaction;

    @ConsumeEvent(value = Bus.CONFIG_UPDATE_LOGGING, ordered = true)
    public void configure(ConfigUpdate event) {
        if (!event.isInitial()) {
            logger.debugf("logging subsystem config update %s", event.getId());
        }
        this.pintleConfig = configProducer.get(event.getId());
    }

    @WithSpan("log response to stdout")
    @ConsumeEvent(Bus.LOG)
    @RunOnVirtualThread
    public void log(QueryContext context) {
        // quick return if not enabled
        if(pintleConfig == null || !pintleConfig.log().enabled() || !pintleConfig.log().stdout()) {
            return;
        }

        final Message question = context.getQuestion();
        final Message answer = context.getAnswer();
        String appended = String.format("[%dms]", context.getElapsedMs());
        if (!context.getExceptions().isEmpty()) {
            logger.errorf("[%s] encountered %d error(s) starting with: %s", context.getTraceId(), context.getExceptions().size(), context.getExceptions().getFirst().getMessage());
        } else if (question != null) {
            logger.infof("[%s] answered question id=%s type=%s name=%s %s", context.getTraceId(), question.getHeader().getID(), Type.string(question.getQuestion().getType()), question.getQuestion().getName().toString(false), appended);
        } else if (answer != null) {
            logger.debugf("[%s] responded with answer id=%s %s", context.getTraceId(), answer.getHeader().getID(), appended);
        }
    }

    @ConsumeEvent(value = Bus.PERSIST_LOG)
    @WithSpan("persist response")
    @Transactional
    @RunOnVirtualThread
    public void logToDatabase(QueryContext context) {
        // quick return if not enabled
        if (pintleConfig == null || !pintleConfig.log().enabled() || !pintleConfig.log().database().enabled()) {
            return;
        }

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
                    m.getSection(Section.ANSWER).forEach(a -> {
                        final AnswerItem answerItem = new AnswerItem();
                        answerItem.logItem = item;
                        answerItem.type = a.getType();
                        answerItem.data = a.rdataToString();
                        item.answers.add(answerItem);
                    });
                }
            });
        }
        item.persist();
    }


}
