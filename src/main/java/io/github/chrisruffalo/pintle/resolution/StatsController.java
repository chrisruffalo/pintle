package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.github.chrisruffalo.pintle.model.QueryResult;
import io.github.chrisruffalo.pintle.model.stats.Client;
import io.github.chrisruffalo.pintle.model.stats.Question;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.xbill.DNS.Message;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class StatsController {

    @ConsumeEvent(value = Bus.UPDATE_QUESTION_STATS)
    @WithSpan("update question stats")
    @Transactional
    @RunOnVirtualThread
    public void updateStats(QueryContext context) {
        final Optional<Message> questionOptional = Optional.ofNullable(context.getQuestion());
        if (questionOptional.isEmpty()) {
            return;
        }

        final Message question = questionOptional.get();
        final int type = question.getQuestion().getType();
        final String hostname = question.getQuestion().getName().toString(false);

        final Question statsQuestion = Question.byTypeAndHostname(type, hostname);

        // update question
        statsQuestion.totalMilliseconds = statsQuestion.totalMilliseconds + context.getElapsedMs();
        statsQuestion.queryCount = statsQuestion.queryCount + 1;
    }

    @Transactional
    public List<Question> getQuestionStats() {
        return Question.findAll().list();
    }

    @Transactional
    public long getQuestionCount() {
        return Question.count();
    }

    @ConsumeEvent(value = Bus.UPDATE_CLIENT_STATS)
    @WithSpan("update client stats")
    @Transactional
    @RunOnVirtualThread
    public void updateClient(QueryContext context) {
        final String clientIp = context.getResponder().toClient();

        final Client client = Client.byAddress(clientIp);

        // another process should insert hostname as needed

        // update queries?
        client.queryCount = client.queryCount + 1;
        client.totalMilliseconds = client.totalMilliseconds + context.getElapsedMs();
        if (QueryResult.ERROR.equals(context.getResult())) {
            client.errors = client.errors + 1;
        }
    }

    @Transactional
    public List<Client> getClientStats() {
        return Client.findAll().list();
    }

}
