package io.github.chrisruffalo.pintle.config;

import io.github.chrisruffalo.pintle.config.diff.Diff;
import io.github.chrisruffalo.pintle.config.diff.Diffable;
import io.github.chrisruffalo.pintle.config.serde.MatcherConverter;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.smallrye.config.WithConverter;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface Group extends Named, Diffable<Group>, NamedComparable<Group> {

    /**
     * A list of resolvers, by name, that clients
     * and queries matched to this group should
     * use to resolve. Generally these resolvers
     * are tried, in order, until a resolution is made.
     *
     * @return if present, a list of resolvers to use for queries to this group. otherwise empty.
     */
    Optional<List<String>> resolvers();

    /**
     * A list of lists, by name, that should
     * be used to perform actions on queries
     * to this group.
     *
     * @return if present, a list of action lists to use for queries to this group. otherwise empty.
     */
    Optional<List<String>> lists();

    /**
     * Matchers provide the logic for matching queries to a group. This
     * list, if present, provides the implementation for the
     * configured mechanisms that match queries to this group.
     *
     * @return if present a list of matchers. otherwise empty;
     */
    @WithConverter(MatcherConverter.class)
    Optional<List<Matcher>> matchers();

    /**
     * This is the entrypoint into the matchers that looks through the
     * matchers and finds if any of the matchers in the matcher "tree" match.
     * The root level functions as an "or" matcher.
     *
     * @param context the current query context to find the group for
     * @return if the current group matches
     */
    default boolean matches(QueryContext context) {
        if(matchers().isPresent() && !matchers().get().isEmpty()) {
            for(final Matcher matcher : matchers().get()) {
                if(matcher.match(context)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    default Diff diff(Group other) {
        final Set<String> diffs = new HashSet<>();
        if (other == null) {
            diffs.add("");
        } else {

        }
        return new Diff(diffs);
    }
}
