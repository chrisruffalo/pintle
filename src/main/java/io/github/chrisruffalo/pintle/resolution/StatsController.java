package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.github.chrisruffalo.pintle.model.stats.Client;
import io.github.chrisruffalo.pintle.model.stats.Question;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.xbill.DNS.Message;
import org.xbill.DNS.Type;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class StatsController {

    @Blocking
    @ConsumeEvent(value = Bus.UPDATE_QUESTION_STATS, ordered = true)
    @WithSpan("update question stats")
    @Transactional
    public void updateStats(QueryContext context) {
        final Optional<Message> questionOptional = Optional.ofNullable(context.getQuestion());
        if (questionOptional.isEmpty()) {
            return;
        }

        final Message question = questionOptional.get();
        final String type = Type.string(question.getQuestion().getType());
        final String hostname = question.getQuestion().getName().toString(true);

        final Question statsQuestion = Question.byTypeAndHostname(type, hostname).orElseGet(() -> {
            final Question q = new Question();
            q.type = type;
            q.hostname = hostname;
            return q;
        });

        // update question
        statsQuestion.totalMilliseconds = statsQuestion.totalMilliseconds + context.getElapsedMs();
        statsQuestion.queryCount = statsQuestion.queryCount + 1;

        statsQuestion.persist();
    }

    @Transactional
    public List<Question> getQuestionStats() {
        return Question.findAll().list();
    }


    @Blocking
    @ConsumeEvent(value = Bus.UPDATE_CLIENT_STATS, ordered = true)
    @WithSpan("update client stats")
    @Transactional
    public void updateClient(QueryContext context) {
        final String clientIp = context.getResponder().toClient();

        final Client client = Client.byAddress(clientIp).orElseGet(() -> {
           final Client c = new Client();
           c.address = clientIp;

           // should resolve hostname?

           return c;
        });

        // should re-resolve hostname?

        // update queries?
        client.queryCount = client.queryCount + 1;

        client.persist();
    }

    @Transactional
    public List<Client> getClientStats() {
        return Client.findAll().list();
    }

}
