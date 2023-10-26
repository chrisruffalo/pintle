package io.github.chrisruffalo.pintle.model.list;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import jdk.jfr.Name;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity(name = "line")
@Table(name = "line")
@NamedQueries({
    @NamedQuery(name = "line.byList", query = "from line where listId = :listId"),
    @NamedQuery(name = "line.byListAndSource", query = "from line where listId = :listId and sourceId = :sourceId"),
    @NamedQuery(name = "line.hostAndResolveByListAndSource", query = "select hostname, resolveTo from line where listId = :listId and sourceId = :sourceId")
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

    public static Set<String> getHostnamesByListAndSource(final long listId, final long sourceId) {
        final PanacheQuery<StoredLine> storedLinesQuery = StoredLine
            .find("#line.hostAndResolveByListAndSource", Parameters.with("sourceId", sourceId).and("listId", listId))
            .project(StoredLine.class)
            ;

        if (storedLinesQuery.count() < 1) {
            return Collections.emptySet();
        }

        return new HashSet<>(storedLinesQuery.list().stream().map(a -> a.hostname + "|" + a.resolveTo).toList());
    }
}
