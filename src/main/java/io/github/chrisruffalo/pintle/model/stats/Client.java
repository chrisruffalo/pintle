package io.github.chrisruffalo.pintle.model.stats;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.*;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Optional;

@Entity(name = "client")
@NamedQueries({
    @NamedQuery(name = "client.byIp", query = "from client where ip = :ip"),
})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Client extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonIgnore
    @Schema(hidden = true)
    public String id;

    public String ip;

    public String hostname;

    @Column(name = "query_count")
    @JsonProperty("query-count" )
    public long queryCount;

    public static Optional<Client> byIp(final String ip) {
        return Client.find("#client.byIp", Parameters.with("ip", ip).map()).stream().findFirst().map(o -> (Client)o);
    }

}
