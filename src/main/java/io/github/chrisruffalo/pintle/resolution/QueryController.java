package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.ResolverType;
import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.github.chrisruffalo.pintle.model.QueryResult;
import io.github.chrisruffalo.pintle.model.ServiceType;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.eventbus.EventBus;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.xbill.DNS.*;
import org.xbill.DNS.dnssec.ValidatingResolver;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

@RequestScoped
public class QueryController {

    @Inject
    Logger logger;

    @Inject
    EventBus eventBus;

    @Inject
    Resolver providedResolver;

    @WithSpan("resolve answer")
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

            // send
            providedResolver.sendAsync(question).whenComplete((resolutionAnswer, resolutionException) -> {
                if(resolutionException != null) {
                    context.getExceptions().add(resolutionException);
                    context.setResult(QueryResult.ERROR);
                    eventBus.send(Bus.HANDLE_ERROR, context);
                } else {
                    context.setResult(QueryResult.RESOLVED);
                    context.setAnswer(resolutionAnswer);
                    eventBus.send(Bus.RESPOND, context);
                }
            });
        }
    }

}
