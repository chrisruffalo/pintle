package io.github.chrisruffalo.pintle.model.stats;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.chrisruffalo.pintle.resource.serde.TypeStringSerializer;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.*;

import java.util.Objects;

@Entity(name = "question")
@NamedQueries({
    @NamedQuery(name = "question.byTypeAndHostname", query = "from question where type = :type and hostname = :hostname"),
})
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Question extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @JsonSerialize(using = TypeStringSerializer.class)
    public int type;

    public String hostname;

    @JsonProperty("total-milliseconds")
    @Column(name = "total_milliseconds")
    public long totalMilliseconds;

    @JsonProperty("query-count")
    @Column(name = "query_count")
    public long queryCount;

    @Transient
    @JsonProperty("average-milliseconds")
    public long getAverageMilliseconds() {
        return queryCount == 0 ? 0 : totalMilliseconds / queryCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Question question = (Question) o;
        return Objects.equals(type, question.type) && Objects.equals(hostname, question.hostname);
    }

    public Question() {

    }

    public Question(int type, String hostname) {
        this.type = type;
        this.hostname = hostname;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, hostname);
    }

    public static Question byTypeAndHostname(final int type, final String hostname) {
        return (Question) Question.find("#question.byTypeAndHostname", Parameters.with("type", type).and("hostname", hostname).map())
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .firstResultOptional()
                .orElseGet(() -> {
                    final Question q = new Question(type, hostname);
                    q.persist();
                    return q;
                });
    }

}
