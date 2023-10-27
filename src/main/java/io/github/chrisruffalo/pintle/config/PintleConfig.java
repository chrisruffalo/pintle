package io.github.chrisruffalo.pintle.config;

import io.github.chrisruffalo.pintle.config.diff.Diff;
import io.github.chrisruffalo.pintle.config.diff.Diffable;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Root configuration object that serves
 * to both allow configuration of pintle components
 * but also allows pintle configuration to
 * integrate with Quarkus configuration.
 *
 */
@ConfigMapping(prefix = "pintle")
public interface PintleConfig extends Diffable<PintleConfig> {

    /**
     * Collection of configuration items that manage
     * storage, locations, and other details.
     *
     * @return the etc configuration
     */
    @WithParentName
    Etc etc();

    Log log();

    @Override
    default PintleConfig unwrap() {
        return this;
    }

    Mdns mdns();

    Optional<List<Group>> groups();

    default Optional<Group> group(final String name) {
        if (groups().isEmpty() || name == null || name.isEmpty()) {
            return Optional.empty();
        }
        return groups().get().stream().filter(g -> name.equals(g.name())).findFirst();
    }

    Optional<List<Listener>> listeners();

    default Optional<Listener> listener(final String name) {
        if (listeners().isEmpty() || name == null || name.isEmpty()) {
            return Optional.empty();
        }
        return listeners().get().stream().filter(l -> name.equals(l.name())).findFirst();
    }

    Optional<List<ActionList>> lists();

    default Optional<ActionList> list(final String name) {
        if (lists().isEmpty() || name == null || name.isEmpty()) {
            return Optional.empty();
        }
        return lists().get().stream().filter(al -> name.equals(al.name())).findFirst();
    }

    Optional<List<Resolver>> resolvers();

    default Optional<Resolver> resolver(final String name) {
        if (resolvers().isEmpty() || name == null || name.isEmpty()) {
            return Optional.empty();
        }
        return resolvers().get().stream().filter(r -> name.equals(r.name())).findFirst();
    }

    @Override
    default Diff diff(PintleConfig other) {
        final Set<String> diffSet = new HashSet<>();
        if (other == null) {
            diffSet.add("");
            if(log() != null) {
                diffSet.add("log");
            }
            if(mdns() != null) {
                diffSet.add("mdns");
            }
            if(groups().isPresent()) {
                diffSet.add("groups");
            }
            if(listeners().isPresent()) {
                diffSet.add("listeners");
            }
            if(lists().isPresent()) {
                diffSet.add("lists");
            }
            if(resolvers().isPresent()) {
                diffSet.add("resolvers");
            }
        } else {
            // add all (relevant) children
            diffSet.addAll(log().diff(other.log()).differences(true));
            diffSet.addAll(mdns().diff(other.mdns()).differences(true));
            diffSet.addAll(Diff.compare("groups", this.groups(), other.groups()).differences());
            diffSet.addAll(Diff.compare("listeners", this.listeners(), other.listeners()).differences());
            diffSet.addAll(Diff.compare("resolvers", this.resolvers(), other.resolvers()).differences());
            diffSet.addAll(Diff.compare("lists", this.lists(), other.lists()).differences());

            if (!diffSet.isEmpty()) {
                diffSet.add("");
            }
        }
        return new Diff("pintleConfig", diffSet);
    }
}
