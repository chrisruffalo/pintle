package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.config.Group;
import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.github.chrisruffalo.pintle.model.QueryResult;
import io.github.chrisruffalo.pintle.resolution.resolver.PintleResolver;
import io.github.chrisruffalo.pintle.resolution.resolver.ResolverHandler;
import io.github.chrisruffalo.pintle.telemetry.SpanController;
import io.github.chrisruffalo.pintle.util.NameUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.xbill.DNS.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@RequestScoped
public class QueryController {

    @Inject
    Logger logger;

    @Inject
    EventBus eventBus;

    @Inject
    ResolverHandler resolverHandler;

    @Inject
    Tracer tracer;

    @Inject
    SpanController spanController;

    private static class ResolverConsumer implements BiConsumer<Message, Throwable> {

        Logger logger = Logger.getLogger(ResolverConsumer.class);

        final Iterator<PintleResolver> it;

        final Span span;

        final EventBus eventBus;

        final QueryContext context;

        public ResolverConsumer(final QueryContext context, Iterator<PintleResolver> it, Span span, EventBus eventBus) {
            this.it = it;
            this.span = span;
            this.eventBus = eventBus;
            this.context = context;
        }

        @Override
        public void accept(Message resolutionAnswer, Throwable resolutionException) {
            logger.infof("consuming %s...", resolutionAnswer);
            final Message question = context.getQuestion();
            final String queryName = NameUtil.string(question.getQuestion().getName());
            if (resolutionException != null) {
                context.getExceptions().add(resolutionException);
                context.setResult(QueryResult.ERROR);
                eventBus.send(Bus.HANDLE_ERROR, context);
            } else if (resolutionAnswer != null) {
                context.setResult(QueryResult.RESOLVED);
                context.setAnswer(resolutionAnswer);
                eventBus.send(Bus.RESPOND, context);
            } else if (it.hasNext()) {
                final PintleResolver resolver = it.next();
                logger.infof("sending to next resolver %s [has next %s]...", resolver.config().name(), it.hasNext());
                resolver.sendAsync(context.getQuestion()).whenComplete(this);
            } else {

            }
            span.end();
        }

    }

    @ConsumeEvent(Bus.QUERY)
    public CompletionStage<Message> resolve(QueryContext context) {
        try (final Scope outer = spanController.startAsCurrent(context)) {
            final Span span = tracer.spanBuilder("query").startSpan();
            try (final Scope scope = span.makeCurrent()) {

                final Message question = context.getQuestion();
                context.getSpan()
                    .setAttribute("pintle.question.body", context.getQuestion().getQuestion().toString())
                    .setAttribute("pintle.question.type", Type.string(context.getQuestion().getQuestion().getType()))
                    .setAttribute("pintle.question.id", String.format("%s", context.getQuestion().getHeader().getID()))
                    .setAttribute("pintle.question.opcode", Opcode.string(context.getQuestion().getHeader().getOpcode()))
                ;

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
                    span.end();
                    return CompletableFuture.completedStage(response);
                } else {
                    logger.debugf("resolving operation: %s", opcode);

                    for (final Group group : context.getGroups()) {
                        final List<PintleResolver> resolversForGroup = resolverHandler.get(group);
                        // if the resolver is null it triggers an NXDOMAIN
                        if (resolversForGroup == null || resolversForGroup.isEmpty()) {
                            logger.warnf("the group '%s' did not have any resolvers, skipping", group.name());
                            continue;
                        }

                        // find the first resolver available for the candidate domain
                        final List<PintleResolver> each = resolversForGroup.stream().filter(Objects::nonNull).filter(pr -> pr.canServiceDomain(queryName)).toList();

                        if (each.isEmpty()) {
                            logger.debugf("the group '%s' did not have any resolvers than can service the domain '%s', skipping", group.name(), NameUtil.string(queryName));
                            continue;
                        }

                        // loop until we get an answer or an error
                        Message answer = null;
                        for (final PintleResolver resolver : each) {
                            try {
                                answer = resolver.send(question);
                                if (answer != null) {
                                    break;
                                }
                            } catch (IOException e) {
                                // continue
                                context.getExceptions().add(e);
                            }
                        }

                        if (context.getExceptions().isEmpty()) {
                            // check answer after loop and set nx domain if the answer is null
                            if (answer == null) {
                                logger.warnf("no resolvers in the groups assigned to the client [%s] could not resolve [%s]", context.getResponder().toClient(), queryName);
                                // nx domain
                                answer = new Message(question.getHeader().getID());
                                answer.getHeader().setFlag(Flags.QR);
                                answer.getHeader().setRcode(Rcode.NXDOMAIN);
                            }
                            // add answer to context
                            context.setResult(QueryResult.RESOLVED);
                            context.setAnswer(answer);
                            eventBus.send(Bus.RESPOND, context);
                        }
                    }
                }

                logger.warnf("groups assigned to the client [%s] could not resolve [%s]", context.getResponder().toClient(), queryName);
                final Message response = new Message(id);
                response.getHeader().setRcode(Rcode.NXDOMAIN);
                response.getHeader().setFlag(Flags.QR);
                eventBus.send(Bus.RESPOND, context);
                span.end();
                return CompletableFuture.completedStage(response);
            }
        }
    }

}
