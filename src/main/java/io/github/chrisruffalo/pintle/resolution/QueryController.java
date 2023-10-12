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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@RequestScoped
public class QueryController {

    @Inject
    Logger logger;

    @Inject
    EventBus eventBus;

    @Inject
    ResolverHandler resolverHandler;

    @WithSpan("resolve answer")
    @ConsumeEvent(Bus.QUERY)
    public CompletionStage<Message> resolve(QueryContext context) throws UnknownHostException {
        final Message question = context.getQuestion();

        final int id = question.getHeader().getID();
        final int opcodeInt = question.getHeader().getOpcode();
        final String opcode = Opcode.string(opcodeInt);
        if (Opcode.QUERY != opcodeInt) {
            final Message response = new Message(id);
            logger.infof("unsupported operation: %s", opcode);
            response.getHeader().setFlag(Flags.QR);
            response.getHeader().setRcode(Rcode.NOTIMP);
            context.setAnswer(response);
            eventBus.send(Bus.RESPOND, context);
            return CompletableFuture.completedStage(response);
        } else {
            logger.debugf("resolving operation: %s", opcode);
            final Resolver resolverForGroup = resolverHandler.get(context.getGroup());
            // if the resolver is null it triggers an NXDOMAIN
            if (resolverForGroup == null) {
                logger.warnf("the group '%s' did not have any resolvers, returning NXDOMAIN", context.getGroup().name());
                final Message response = new Message(id);
                response.getHeader().setRcode(Rcode.NXDOMAIN);
                response.getHeader().setFlag(Flags.QR);
                eventBus.send(Bus.RESPOND, context);
                return CompletableFuture.completedStage(response);
            }
            // send
            return resolverForGroup.sendAsync(question).whenCompleteAsync((resolutionAnswer, resolutionException) -> {
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
