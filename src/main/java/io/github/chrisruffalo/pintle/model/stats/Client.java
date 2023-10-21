package io.github.chrisruffalo.pintle.model.stats;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.*;

import java.util.Objects;

@Entity(name = "client")
@NamedQueries({
    @NamedQuery(name = "client.byAddress", query = "from client where address = :address"),
})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Client extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String address;

    @Column(name = "query_count")
    @JsonProperty("query-count" )
    public long queryCount;

    @Column(name = "total_milliseconds")
    @JsonProperty("total-elapsed-milliseconds")
    public long totalMilliseconds;

    @Column(name = "error_count")
    public long errors;

    @Transient
    @JsonProperty("average-milliseconds")
    public long getAverageMilliseconds() {
        return queryCount == 0 ? 0 : totalMilliseconds / queryCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Client client = (Client) o;
        return Objects.equals(address, client.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    public static Client byAddress(final String address) {
        return (Client) Client.find("#client.byAddress", Parameters.with("address", address).map())
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .firstResultOptional()
                .orElseGet(() -> {
                    final Client c = new Client();
                    c.address = address;
                    c.persist();
                    return c;
                });
    }

}
