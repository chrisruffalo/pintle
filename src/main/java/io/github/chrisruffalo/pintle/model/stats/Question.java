package io.github.chrisruffalo.pintle.model.stats;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.chrisruffalo.pintle.resource.serde.TypeStringSerializer;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.*;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Objects;
import java.util.Optional;

@Entity(name = "question")
@NamedQueries({
    @NamedQuery(name = "question.byTypeAndHostname", query = "from question where type = :type and hostname = :hostname"),
})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Question extends PanacheEntityBase {

    @Id
    @JsonSerialize(using = TypeStringSerializer.class)
    public int type;

    @Id
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
        return totalMilliseconds / queryCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Question question = (Question) o;
        return Objects.equals(type, question.type) && Objects.equals(hostname, question.hostname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, hostname);
    }

    public static Optional<Question> byTypeAndHostname(final int type, final String hostname) {
        return Question.find("#question.byTypeAndHostname", Parameters.with("type", type).and("hostname", hostname).map()).stream().findFirst().map(o -> (Question)o);
    }

}
