package io.github.chrisruffalo.pintle.model.list;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;

import java.util.Objects;

@Entity(name = "line")
@Table(name = "line")
@NamedQueries({
    @NamedQuery(name = "line.byList", query = "from line where id = :listId"),
    @NamedQuery(name = "line.byListAndSource", query = "from line where listId = :listId and sourceId = :sourceId")
})
public class StoredLine extends PanacheEntityBase {

    /**
     * The list identifier
     */
    @Column(insertable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long id;

    /**
     * The source it comes from
     */
    @Id
    @Column(name="source_id")
    public long sourceId;

    @Id
    @Column(name="list_id")
    public long listId;

    /**
     * The hostname that should be blocked
     */
    @Id
    public String hostname;

    /**
     * What it should resolve to.
     */
    @Column(name = "resolve_to")
    public String resolveTo;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoredLine line = (StoredLine) o;
        return Objects.equals(id, line.id) && Objects.equals(hostname, line.hostname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, hostname);
    }

    @RunOnVirtualThread
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public static StoredLine createOrGetByListSourceAndHostname(final long listId, final long sourceId, final String hostname, final String resolveTo) {
        final StoredLine storedLine = (StoredLine)StoredLine.find(
            "listId = :listId and sourceId = :sourceId and hostname = :hostname",
                  Parameters.with("listId", listId).and("sourceId", sourceId).and("hostname", hostname)
                ).firstResultOptional()
                .orElseGet(() -> {
                    final StoredLine line = new StoredLine();
                    line.listId = listId;
                    line.sourceId = sourceId;
                    line.hostname = hostname;
                    line.resolveTo = resolveTo;
                    line.persist();
                    return line;
                });
        if ((storedLine.resolveTo == null && resolveTo != null) || !storedLine.resolveTo.equals(resolveTo)) {
            storedLine.resolveTo = resolveTo;
        }
        return storedLine;
    }
}
