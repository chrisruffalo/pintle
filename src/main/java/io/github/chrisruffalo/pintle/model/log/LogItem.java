package io.github.chrisruffalo.pintle.model.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.chrisruffalo.pintle.model.QueryResult;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.ZonedDateTime;

@Entity(name = "log_item")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class LogItem extends PanacheEntity {

    @Column(length = 50)
    public String service;

    @JsonProperty("client-ip")
    @Column(name = "client_ip", length = 45)
    public String clientIp;

    @Column(length = 10)
    public String type;

    @Column(columnDefinition = "text")
    public String hostname;

    @Column
    @Enumerated(EnumType.STRING)
    public QueryResult result;

    @JsonProperty("rcode")
    @Column(name = "rcode", length = 10)
    public String responseCode;

    /**
     * When the query context is first created
     * the time is recorded. This is the first
     * time that the query is noted.
     */
    @JsonProperty("start-time")
    @Column(name = "start_time")
    public ZonedDateTime start;

    /**
     * When the query is finally responded to.
     */
    @JsonProperty("end-time")
    @Column(name = "end_time")
    public ZonedDateTime end;

    @JsonProperty("elapsed-time")
    @Column(name = "elapsed_time")
    public long elapsedTime;

    @Lob
    public byte[] answer;
}
