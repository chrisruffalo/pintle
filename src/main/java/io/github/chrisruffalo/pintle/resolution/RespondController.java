package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.resolution.dto.QueryContext;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import java.net.UnknownHostException;

@ApplicationScoped
public class RespondController {

    @Inject
    EventBus bus;

    @Inject
    Logger logger;

    @ConsumeEvent(Bus.RESPOND)
    public void respond(QueryContext context) throws UnknownHostException {
        final Message question = context.getQuestion();
        Message answer = context.getAnswer();

        // cover for null answers
        if (answer == null) {
            answer = new Message();
            answer.getHeader().setFlag(Flags.QR);
            if(question != null) {
                answer.getHeader().setID(question.getHeader().getID());
            }
            answer.getHeader().setRcode(Rcode.NXDOMAIN);
        }

        context.getResponder().respond(answer).onComplete((asyncResult) -> {
            bus.send(Bus.LOG, context);
            bus.send(Bus.UPDATE_CACHE, context);
        });
    }

    @ConsumeEvent(Bus.HANDLE_ERROR)
    public void handleError(QueryContext context) throws UnknownHostException {
        // turn error into response
        final Message errorAnswer = new Message();
        if (context.getQuestion() != null) {
            errorAnswer.getHeader().setID(context.getQuestion().getHeader().getID());
        }
        errorAnswer.getHeader().setRcode(Rcode.SERVFAIL);
        errorAnswer.getHeader().setFlag(Flags.QR);

        bus.send(Bus.RESPOND, context);
        bus.send(Bus.LOG, context);
    }

}
