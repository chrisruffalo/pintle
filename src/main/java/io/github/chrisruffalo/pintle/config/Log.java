package io.github.chrisruffalo.pintle.config;

import io.github.chrisruffalo.pintle.config.diff.Diff;
import io.github.chrisruffalo.pintle.config.diff.Diffable;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * In the context of Pintle queries this configuration
 * provides control over where access/query logs go
 * and what is stored there. (And for how long.)
 *
 */
public interface Log extends Diffable<Log> {

    /**
     * If this is false then no query logging
     * will be performed.
     *
     * @return
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The sample percent allows control over how often
     * queries are selected for logging. A value of 100
     * means that all queries will, 50 would mean half of
     * the queries would be, and 0 means no queries
     * would be logged. This applies to stdout as well
     * as database logging.
     *
     * @return the percentage, as an integer, of queries that should be selected for logging
     */
    @WithName("sample-percent")
    @WithDefault("100")
    int samplePercent();

    /**
     * Defines if queries should be logged to stdout.
     *
     * @return true if they should be, false otherwise
     */
    @WithDefault("false")
    boolean stdout();

    /**
     * An entire section to define the behavior of the
     * access log/query log database.
     *
     * @return the configuraiton for the database section
     */
    Database database();

    /**
     * Controls the configuration of the query log database.
     */
    interface Database extends Diffable<Database>, Serializable {
        /**
         * If not enabled then the database will be created
         * but not used. No queries will be logged.
         *
         * @return true if the database should be used for storing queries, false otherwise
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * Storing the answers to a query takes up a fair amount of disk space in
         * addition to the basic query log. Disabling answer logging reduces
         * the size of the database.
         *
         * @return true if answers should be stored for each logged query, false otherwise
         */
        @WithDefault("true")
        boolean answers();

        /**
         * Defiens how many days that a query log entry should be saved.
         *
         * @return the number of days a query log entry should be saved
         */
        @WithName("retention-days")
        @WithDefault("10")
        int retentionDays();

        @Override
        default Diff diff(Database other) {
            final Set<String> diffs = new HashSet<>();
            if (other == null) {
                diffs.add("");
                diffs.add("enabled");
                diffs.add("answers");
                diffs.add("retentionDays");
            } else {
                if (!Objects.equals(this.enabled(), other.enabled())) {
                    diffs.add("enabled");
                }
                if (!Objects.equals(this.answers(), other.answers())) {
                    diffs.add("answers");
                }
                if (this.retentionDays() != other.retentionDays()) {
                    diffs.add("retentionDays");
                }
                if (!diffs.isEmpty()) {
                    diffs.add("");
                }
            }
            return new Diff("database", diffs);
        }
    }

    @Override
    default Diff diff(Log other) {
        final Set<String> diffs = new HashSet<>();
        if (other == null) {
            diffs.add("");
            diffs.add("enabled");
            diffs.add("samplePercent");
            diffs.add("stdout");
        } else {
            if (this.enabled() != other.enabled()) {
                diffs.add("enabled");
            }
            if (!Objects.equals(this.samplePercent(), other.samplePercent())) {
                diffs.add("samplePercent");
            }
            if (!Objects.equals(this.stdout(), other.stdout())) {
                diffs.add("stdout");
            }
            if (database() != null) {
                diffs.addAll(database().diff(other.database()).differences());
            }
            if (!diffs.isEmpty()) {
                diffs.add("");
            }
        }
        return new Diff("log", diffs);
    }
}
