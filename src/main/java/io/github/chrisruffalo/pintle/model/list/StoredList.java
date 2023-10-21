package io.github.chrisruffalo.pintle.model.list;

import io.github.chrisruffalo.pintle.config.ActionList;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.*;

import java.util.Optional;

/**
 * The db-stored parts of the list
 */
@Entity(name = "list")
@Table(name = "list")
@NamedQueries({
    @NamedQuery(name = "list.byName", query = "from list where name = :name"),
})
public class StoredList extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /**
     * The name of the list (from the configuration)
     */
    public String name;

    /**
     * Probably doesn't need to be stored but
     * can be referenced here without finding
     * the configuration and linking them.
     */
    public ActionList.Action action;

    /**
     * Again, can be referenced from here
     * without looking for the configuration
     * and linking them.
     */
    public ActionList.Type type;

    /**
     * The id of the last applied configuration
     */
    @Column(name = "last_configuration_id")
    public String lastConfiguration;

    @Column(name = "cache_hash")
    public String cacheHash;

    @Transient
    public long entries;

    public static Optional<StoredList> byName(final String name) {
        return StoredList.find("#list.byName", Parameters.with("name", name)).firstResultOptional();
    }

}
