package io.github.chrisruffalo.pintle.model.stats;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.*;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Optional;

@Entity(name = "question")
@NamedQueries({
    @NamedQuery(name = "question.byTypeAndHostname", query = "from question where type = :type and hostname = :hostname"),
})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Question extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonIgnore
    @Schema(hidden = true)
    public String id;

    public String type;

    public String hostname;

    @JsonProperty("average-millisecond")
    @Column(name = "average_milliseconds")
    public long averageMillisecond;

    @JsonProperty("query-count")
    @Column(name = "query_count")
    public long queryCount;

    public static Optional<Question> byTypeAndHostname(final String type, final String hostname) {
        return Question.find("#question.byTypeAndHostname", Parameters.with("type", type).and("hostname", hostname).map()).stream().findFirst().map(o -> (Question)o);
    }

}
