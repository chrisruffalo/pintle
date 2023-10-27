package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.config.Group;
import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.github.chrisruffalo.pintle.model.QueryResult;
import io.github.chrisruffalo.pintle.resolution.resolver.PintleResolver;
import io.github.chrisruffalo.pintle.resolution.resolver.ResolverHandler;
import io.github.chrisruffalo.pintle.util.NameUtil;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.xbill.DNS.*;

import java.util.List;
import java.util.Optional;
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
    public CompletionStage<Message> resolve(QueryContext context) {
        final Message question = context.getQuestion();

        final int id = question.getHeader().getID();
        final Name queryName = context.getQuestion().getQuestion().getName();
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

            for(final Group group : context.getGroups()) {
                final List<PintleResolver> resolversForGroup = resolverHandler.get(group);
                // if the resolver is null it triggers an NXDOMAIN
                if (resolversForGroup == null || resolversForGroup.isEmpty()) {
                    logger.warnf("the group '%s' did not have any resolvers, skipping", group.name());
                    continue;
                }

                // find the first resolver available for the candidate domain
                final Optional<PintleResolver> resolver = resolversForGroup.stream().filter(pr -> pr.canServiceDomain(queryName)).findFirst();

                if (resolver.isEmpty()) {
                    logger.debugf("the group '%s' did not have any resolvers than can service the domain '%s', skipping", group.name(), NameUtil.string(queryName));
                    continue;
                }
                logger.debugf("using resolver %s", resolver.get().config().name());
                return resolver.get().sendAsync(question).whenCompleteAsync((resolutionAnswer, resolutionException) -> {
                    if (resolutionException != null) {
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

        logger.warnf("groups assigned to the client [%s] could not resolve [%s]", context.getResponder().toClient(), queryName);
        final Message response = new Message(id);
        response.getHeader().setRcode(Rcode.NXDOMAIN);
        response.getHeader().setFlag(Flags.QR);
        eventBus.send(Bus.RESPOND, context);
        return CompletableFuture.completedStage(response);
    }

}
