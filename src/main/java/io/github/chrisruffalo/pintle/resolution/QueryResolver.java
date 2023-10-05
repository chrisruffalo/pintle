package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.resolution.dto.QueryContext;
import io.github.chrisruffalo.pintle.event.Bus;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.xbill.DNS.*;

import java.net.UnknownHostException;

@RequestScoped
public class QueryResolver {

    @Inject
    Logger logger;

    @Inject
    EventBus eventBus;

    @ConsumeEvent(Bus.QUERY)
    public void resolve(QueryContext context) throws UnknownHostException {
        final Message question = context.getQuestion();

        final int opcodeInt = question.getHeader().getOpcode();
        final String opcode = Opcode.string(opcodeInt);
        if (Opcode.QUERY != opcodeInt) {
            final int id = question.getHeader().getID();
            final Message response = new Message(id);
            logger.infof("unsupported operation: %s", opcode);
            response.getHeader().setFlag(Flags.QR);
            response.getHeader().setRcode(Rcode.NOTIMP);
            context.setAnswer(response);
            eventBus.send(Bus.RESPOND, context);
        } else {
            logger.debugf("resolving operation: %s", opcode);

            // create resolver
            final Resolver r = new SimpleResolver("8.8.8.8");

            // send
            r.sendAsync(question).whenComplete((resolutionAnswer, resolutionException) -> {
                if(resolutionException != null) {
                    context.getExceptions().add(resolutionException);
                    eventBus.send(Bus.HANDLE_ERROR, context);
                } else {
                    context.setAnswer(resolutionAnswer);
                    eventBus.send(Bus.RESPOND, context);
                }
            });
        }
    }

}
