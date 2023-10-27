package io.github.chrisruffalo.pintle.model.list;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.*;

import java.util.Objects;

@Entity(name = "line")
@Table(name = "line")
@NamedQueries({
    @NamedQuery(name = "line.byList", query = "from line where listId = :listId"),
    @NamedQuery(name = "line.byListAndSource", query = "from line where listId = :listId and sourceId = :sourceId"),
    @NamedQuery(name = "line.hostAndResolveByListAndSource", query = "select hostname, resolveTo from line where listId = :listId and sourceId = :sourceId"),
    @NamedQuery(name = "line.byListAndSourceAndVersion", query = "from line where listId = :listId and sourceId = :sourceId and version = :sourceVersion")
})
@RegisterForReflection
public class StoredLine extends PanacheEntityBase {

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
    @Id
    @Column(name = "resolve_to")
    public String resolveTo;

    @Id
    @Column(name = "source_version")
    public long version = 1;

    public StoredLine() {

    }

    /**
     * Constructor used by panache to project a subset of
     * information when only the hostname and resolution
     * values are needed
     *
     * @param hostname to respond to
     * @param resolveTo the data to resolve the hostname to
     */
    public StoredLine(String hostname, String resolveTo) {
        this.hostname = hostname;
        this.resolveTo = resolveTo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoredLine line = (StoredLine) o;
        return Objects.equals(listId, line.listId) && Objects.equals(sourceId, line.sourceId) && Objects.equals(hostname, line.hostname) && Objects.equals(resolveTo, line.resolveTo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(listId, sourceId, hostname, resolveTo);
    }

    public static long getCountForListSourceVersion(final long listId, final long sourceId, final long sourceVersion) {
        final PanacheQuery<StoredLine> storedLinesQuery = StoredLine
            .find("#line.byListAndSourceAndVersion",
                    Parameters.with("sourceId", sourceId).and("listId", listId).and("sourceVersion", sourceVersion))
            ;

        return storedLinesQuery.count();
    }
}
