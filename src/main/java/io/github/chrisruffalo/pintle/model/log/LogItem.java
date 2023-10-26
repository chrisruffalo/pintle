package io.github.chrisruffalo.pintle.model.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.chrisruffalo.pintle.model.QueryResult;
import io.github.chrisruffalo.pintle.model.ServiceType;
import io.github.chrisruffalo.pintle.resource.serde.RcodeStringSerializer;
import io.github.chrisruffalo.pintle.resource.serde.TypeStringSerializer;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.*;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;

@Entity
@Table(name = "log_item")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@RegisterForReflection
public class LogItem extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column
    @Enumerated(EnumType.ORDINAL)
    public ServiceType service;

    @JsonProperty("client-address")
    public String clientAddress;

    @Column
    @JsonSerialize(using = TypeStringSerializer.class)
    public int type;

    @Column(columnDefinition = "text")
    public String hostname;

    @Column
    @Enumerated(EnumType.ORDINAL)
    public QueryResult result;

    @JsonProperty("rcode")
    @Column(name = "rcode")
    @JsonSerialize(using = RcodeStringSerializer.class)
    public int responseCode;

    /**
     * When the query context is first created
     * the time is recorded. This is the first
     * time that the query is noted.
     */
    @JsonProperty("start-time")
    @Column(name = "start_time")
    public ZonedDateTime start;

    @JsonProperty("elapsed-time")
    @Column(name = "elapsed_time")
    public int elapsedTime;

    @OneToMany(mappedBy = "logItem", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    public List<AnswerItem> answers = new LinkedList<>();

    @Transient
    @JsonProperty("end-time")
    public ZonedDateTime end() {
        return start.plus(elapsedTime, ChronoUnit.MILLIS);
    }
}
