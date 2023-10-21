package io.github.chrisruffalo.pintle.model.list;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.ZonedDateTime;

@Entity(name = "source")
@Table(name = "source")
@NamedQueries({
    @NamedQuery(name = "source.byUri", query = "from source where uri = :uri"),
})
public class StoredSource extends PanacheEntityBase {

    @Id
    public String uri;

    @Column(insertable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

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

}
