package io.github.chrisruffalo.pintle.model.stats;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.*;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Optional;

@Entity(name = "client")
@NamedQueries({
    @NamedQuery(name = "client.byAddress", query = "from client where address = :address"),
})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Client extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonIgnore
    @Schema(hidden = true)
    public String id;

    public String address;

    @Column(name = "query_count")
    @JsonProperty("query-count" )
    public long queryCount;

    public static Optional<Client> byAddress(final String address) {
        return Client.find("#client.byAddress", Parameters.with("address", address).map()).stream().findFirst().map(o -> (Client)o);
    }

}
