package io.github.chrisruffalo.pintle.model.list;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.*;

import java.time.ZonedDateTime;
import java.util.Objects;

@Entity(name = "source")
@Table(name = "source")
@NamedQueries({
    @NamedQuery(name = "source.byUri", query = "from source where uri = :uri"),
})
@RegisterForReflection
public class StoredSource extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column
    public String uri;

    /**
     * Path, relative to the cache directory,
     * that the actual file is stored/saved
     */
    @Column(name = "cache_path")
    public String dataPath;

    /**
     * Used as server-side controlled hashing for
     * values sent
     */
    @Column
    public String etag;

    @Column
    public String hash;

    @Column
    public String compression;

    /**
     * Cache the value at least until the given time
     */
    @Column(name = "cache_until")
    public ZonedDateTime cacheUntil;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoredSource that = (StoredSource) o;
        return Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }
}
