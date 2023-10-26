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
     * The version of the source/resource. This is
     * not a JPA version but is managed by the application
     */
    @Column(nullable = false)
    public long version = 0;

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

    /**
     * The hash of the file itself.
     */
    @Column
    public String hash;

    /**
     * The hash of the contents of the file. If the
     * file is compressed or contained this would
     * be different from the file hash. Otherwise
     * expect them to be the same value.
     */
    @Column(name = "content_hash")
    public String contentHash;

    /**
     * The compression scheme, usually from the
     * http download, like "gzip".
     */
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
