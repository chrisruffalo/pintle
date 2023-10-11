package io.github.chrisruffalo.pintle.model.log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.chrisruffalo.pintle.resource.serde.TypeStringSerializer;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

/**
 * These are the only parts of an answer
 * that we particularly care about.
 */
@Entity
@Table(name = "log_answer")
@JsonIgnoreProperties(value = {"id"})
public class AnswerItem extends PanacheEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_item_id")
    public LogItem logItem;

    @JsonSerialize(using = TypeStringSerializer.class)
    public int type;

    public String data;

}
