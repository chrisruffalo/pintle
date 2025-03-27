package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.github.chrisruffalo.pintle.model.QueryResult;
import io.github.chrisruffalo.pintle.model.stats.Client;
import io.github.chrisruffalo.pintle.model.stats.Question;
import io.github.chrisruffalo.pintle.telemetry.SpanController;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.xbill.DNS.Message;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class StatsController extends PostRespondController {

    @Inject
    Tracer tracer;

    @Inject
    SpanController spanController;

    public void init(@Observes StartupEvent start){
        // has 2 post-query events
        this.register(2);
    }

    @ConsumeEvent(value = Bus.QUERY_DONE)
    @Transactional
    @RunOnVirtualThread
    public void updateStats(QueryContext context) {
        final Optional<Message> questionOptional = Optional.ofNullable(context.getQuestion());
        if (questionOptional.isEmpty()) {
            if (done(context)){
                context.getSpan().end();
            }
            return;
        }

        try (final Scope outer = spanController.startAsCurrent(context)) {
            final Span span = tracer.spanBuilder("update-query-stats").startSpan();
            try (final Scope scope = span.makeCurrent()) {
                final Message question = questionOptional.get();
                final int type = question.getQuestion().getType();
                final String hostname = question.getQuestion().getName().toString(false);

                final Question statsQuestion = Question.byTypeAndHostname(type, hostname);

                // update question
                statsQuestion.totalMilliseconds = statsQuestion.totalMilliseconds + context.getElapsedMs();
                statsQuestion.queryCount = statsQuestion.queryCount + 1;
            } finally {
                span.end();
            }
        }

        if (done(context)){
            context.getSpan().end();
        }
    }

    @Transactional
    public List<Question> getQuestionStats() {
        return Question.findAll().list();
    }

    @Transactional
    public long getQuestionCount() {
        return Question.count();
    }

    @ConsumeEvent(value = Bus.QUERY_DONE)
    @Transactional
    @RunOnVirtualThread
    public void updateClient(QueryContext context) {
        try (final Scope outer = spanController.startAsCurrent(context)) {
            final Span span = tracer.spanBuilder("update-client-stats").startSpan();
            try (final Scope scope = span.makeCurrent()) {
                final String clientIp = context.getResponder().toClient();

                final Client client = Client.byAddress(clientIp);

                // another process should insert hostname as needed

                // update queries?
                client.queryCount = client.queryCount + 1;
                client.totalMilliseconds = client.totalMilliseconds + context.getElapsedMs();
                if (QueryResult.ERROR.equals(context.getResult())) {
                    client.errors = client.errors + 1;
                }
            } finally {
                span.end();
            }
        }

        if (done(context)){
            context.getSpan().end();
        }
    }

    @Transactional
    public List<Client> getClientStats() {
        return Client.findAll().list();
    }

}
