package io.github.chrisruffalo.pintle.model.log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.chrisruffalo.pintle.model.QueryResult;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.ZonedDateTime;

@Entity(name = "log_item")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class LogItem extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(hidden = true)
    @JsonIgnore
    public String id;

    @Column(name = "trace")
    public String trace;

    @Column
    @Enumerated(EnumType.STRING)
    public QueryResult result;

    /**
     * When the query context is first created
     * the time is recorded. This is the first
     * time that the query is noted.
     */
    @Column(name = "start_time")
    public ZonedDateTime start;

    /**
     * When the query is finally responded to.
     */
    @Column(name = "end_time")
    public ZonedDateTime end;

    @Column(name = "elapsed_time")
    public long elapsedTime;

    @Lob
    public byte[] question;

    @Lob
    public byte[] answer;
}
