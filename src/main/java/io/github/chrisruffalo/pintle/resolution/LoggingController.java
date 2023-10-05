package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.resolution.dto.QueryContext;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.xbill.DNS.Message;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.net.UnknownHostException;

@ApplicationScoped
public class LoggingController {

    @Inject
    Logger logger;

    @ConsumeEvent(Bus.LOG)
    public void log(QueryContext context) throws UnknownHostException {
        String answerText = "";
        final Message question = context.getQuestion();
        final Message answer = context.getAnswer();
        if (answer != null) {
            answerText = "(" + answer.getSection(Section.ANSWER).get(0).toString() + ")";
        }
        if (question != null) {
            logger.infof("answered question id=%s type=%s name=%s %s", question.getHeader().getID(), Type.string(question.getQuestion().getType()), question.getQuestion().getName().toString(false), answerText);
        } else if(answer != null) {
            logger.infof("responded with answer id=%s %s", answer.getHeader().getID(), answerText);
        }
    }

}
