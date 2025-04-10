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
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.hibernate.StatelessSession;
import org.xbill.DNS.Message;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class StatsController extends PostRespondController {

    @Inject
    Tracer tracer;

    @Inject
    SpanController spanController;

    @Inject
    @PersistenceUnit("stats-db")
    StatelessSession statelessSession;

    final Queue<Client> clientUpdateQueue = new ConcurrentLinkedQueue<>();
    final Queue<Question> questionUpdateQueue = new ConcurrentLinkedQueue<>();

    public void init(@Observes StartupEvent start){
        // has 2 post-query events
        this.register(2);
    }

    @ConsumeEvent(value = Bus.QUERY_DONE)
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

                final Question questionStats = new Question();
                questionStats.hostname = hostname;
                questionStats.type = type;
                questionStats.totalMilliseconds = context.getElapsedMs();
                questionStats.queryCount = 1;

                questionUpdateQueue.add(questionStats);
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
    @RunOnVirtualThread
    public void updateClient(QueryContext context) {
        try (final Scope outer = spanController.startAsCurrent(context)) {
            final Span span = tracer.spanBuilder("update-client-stats").startSpan();
            try (final Scope scope = span.makeCurrent()) {
                final String clientIp = context.getResponder().toClient();
                int errors = QueryResult.ERROR.equals(context.getResult()) ? 1 : 0;

                final Client client = new Client();
                client.address = clientIp;
                client.queryCount = 1;
                client.totalMilliseconds = context.getElapsedMs();
                client.errors = errors;

                clientUpdateQueue.add(client);
            } finally {
                span.end();
            }
        }

        if (done(context)) {
            context.getSpan().end();
        }
    }


    @Transactional
    public List<Client> getClientStats() {
        return Client.findAll().list();
    }

    /**
     * Drains the queue into the given list. This allows
     * us to get an accurate copy of the queue while
     * working around any race conditions and having a
     * clean cutoff point so the queue can refill.
     *
     * @param source to drain (side effect: queue will be empty when done)
     * @return the contents of the queue, as a list
     * @param <T> content type of input queue
     */
    <T> List<T> drain(Queue<T> source) {
        final List<T> output = new LinkedList<>();
        while (!source.isEmpty()) {
            output.add(source.remove());
        }
        return output;
    }

    @Transactional
    @Scheduled(every = "1s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void flushQuestionQueue() {
        if (questionUpdateQueue.isEmpty()) {
            return;
        }

        final List<Question> toUpdate = drain(this.questionUpdateQueue);

        final Map<String, Question> questions = new HashMap<>();
        toUpdate.forEach(question -> {
            final String key = String.format("%s|%s",question.hostname,question.type);
            if (!questions.containsKey(key)) {
                questions.put(key, question);
            } else {
                final Question existing = questions.get(key);
                existing.totalMilliseconds += question.totalMilliseconds;
                existing.queryCount += question.queryCount;
            }
        });

        final AtomicLong totalRows = new AtomicLong(0);
        questions.forEach((key, value) -> {
            final long rows = statelessSession.createMutationQuery("insert into question (hostname,type,queryCount,totalMilliseconds) values (:hostname,:type,:queryCount,:incMillis) on conflict(hostname,type) do update set queryCount = excluded.queryCount + queryCount, excluded.totalMilliseconds = excluded.totalMilliseconds + totalMilliseconds")
                    .setParameter("type", value.type)
                    .setParameter("hostname", value.hostname)
                    .setParameter("queryCount", value.queryCount)
                    .setParameter("incMillis", value.totalMilliseconds)
                    .executeUpdate();
            totalRows.getAndAdd(rows);
        });
        logger.tracef("updated stats for %d hostname/type", totalRows.get());
    }

    @Transactional
    @Scheduled(every = "1s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void flushClientQueue() {
        if (clientUpdateQueue.isEmpty()) {
            return;
        }

        final List<Client> toUpdate = drain(this.clientUpdateQueue);

        final Map<String, Client> clients = new HashMap<>();
        toUpdate.forEach(client -> {
            if (!clients.containsKey(client.address)) {
                clients.put(client.address, client);
            } else {
                final Client existing = clients.get(client.address);
                existing.errors += client.errors;
                existing.totalMilliseconds += client.totalMilliseconds;
                existing.queryCount += client.queryCount;
            }
        });

        final AtomicLong totalRows = new AtomicLong(0);
        clients.forEach((key, value) -> {
            final long rows = statelessSession.createMutationQuery("insert into client (address,queryCount,errors,totalMilliseconds) values (:address,:queryCount,:errors,:incMillis) on conflict(address) do update set queryCount = excluded.queryCount + queryCount, excluded.errors = excluded.errors + errors, excluded.totalMilliseconds = excluded.totalMilliseconds + totalMilliseconds")
                    .setParameter("address", value.address)
                    .setParameter("queryCount", value.queryCount)
                    .setParameter("incMillis", value.totalMilliseconds)
                    .setParameter("errors", value.errors)
                    .executeUpdate();
            totalRows.getAndAdd(rows);
        });
        logger.tracef("updated stats for %d clients", totalRows.get());
    }

}
