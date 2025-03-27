package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.github.chrisruffalo.pintle.telemetry.SpanController;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.logging.Logger;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Section;

import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class RespondController {

    @Inject
    EventBus bus;

    @Inject
    Logger logger;

    @Inject
    Tracer tracer;

    @Inject
    SpanController spanController;

    @ConsumeEvent(Bus.RESPOND)
    @Retry
    @Fallback(fallbackMethod = "fallback")
    @RunOnVirtualThread
    public CompletionStage<Void> respond(QueryContext context) {
        final Message question = context.getQuestion();
        Message answer = context.getAnswer();

        try (final Scope outer = spanController.startAsCurrent(context)) {
            final Span span = tracer.spanBuilder("respond").startSpan();
            try (final Scope scope = span.makeCurrent()) {

                // cover for null answers
                if (answer == null) {
                    answer = new Message();
                    answer.getHeader().setFlag(Flags.QR);
                    if (question != null) {
                        answer.getHeader().setID(question.getHeader().getID());
                    }
                    answer.getHeader().setRcode(Rcode.NXDOMAIN);
                }
                final Message finalAnswer = answer;

                return context.getResponder().respond(finalAnswer).toCompletionStage().whenComplete((voidResult, throwable) -> {
                    context.getSpan().setAttribute("pintle.response.body", finalAnswer.sectionToString(Section.ANSWER));
                    context.getSpan().setAttribute("pintle.response.code", Rcode.string(finalAnswer.getRcode()));
                    dissem(context);
                }).thenRun(span::end);
            }
        }
    }

    @ConsumeEvent(Bus.HANDLE_ERROR)
    @RunOnVirtualThread
    public void handleError(QueryContext context) throws UnknownHostException {
        // turn error into response
        final Message errorAnswer = new Message();
        if (context.getQuestion() != null) {
            errorAnswer.getHeader().setID(context.getQuestion().getHeader().getID());
        }
        errorAnswer.getHeader().setRcode(Rcode.SERVFAIL);
        errorAnswer.getHeader().setFlag(Flags.QR);

        bus.send(Bus.RESPOND, context);
    }

    /**
     * If the `respond` method fails to respond then
     * take the exception and run it through the
     * error handler.
     *
     * @param context of the query
     * @param throwable that potentially caused the issue
     */
    @RunOnVirtualThread
    public  CompletionStage<Void> fallback(QueryContext context, Throwable throwable){
        context.getExceptions().add(throwable);

        // todo: set the retry threshold
        if (context.getExceptions().size() > 3) {
            dissem(context);
            return CompletableFuture.runAsync(() -> {});
        }

        bus.send(Bus.HANDLE_ERROR, context);
        return CompletableFuture.runAsync(() -> {});
    }

    /**
     * Send the query context out to the final destinations that
     * need it (logging, statistics, etc).
     *
     * @param context to disseminate the final state of query
     */
    private void dissem(QueryContext context) {
        // todo: this probably should be finished and not responded
        //       because it could error out without ever sending
        //       anything back
        context.setResponded(ZonedDateTime.now());

        // publish to all the controllers that handle the end-state
        // of the context.
        bus.publish(Bus.QUERY_DONE, context);
    }
}
