package dev.silentcraft.tools.spring.test.context.cache;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.test.context.MergedContextConfiguration;

/**
 * Holds the complete history of context cache misses for a given test class.
 * <p>
 * This record is used to store one or more {@link Entries} corresponding to
 * each observed cache miss. It is immutable, with each new entry generating
 * a new instance via {@link #withNew(Entries)}.
 *
 * @param entries the list of recorded context cache miss entries.
 */
public record CacheMissInfo(List<Entries> entries) {

    /**
     * Creates a new immutable {@code CacheMissInfo} instance.
     *
     * @param entries the list of cache miss entries.
     */
    public CacheMissInfo(List<Entries> entries) {
        this.entries = List.copyOf(entries);
    }

    /**
     * Creates a new {@code CacheMissInfo} with the first recorded entry.
     *
     * @param entries the initial entry.
     * @return a new {@code CacheMissInfo} instance.
     */
    public static CacheMissInfo withFirst(Entries entries) {
        return new CacheMissInfo(List.of(entries));
    }

    @Override
    public List<Entries> entries() {
        return List.copyOf(entries);
    }

    public void addDetail(Entries detail) {
        entries.add(detail);
    }

    /**
     * Returns a new {@code CacheMissInfo} instance with an additional entry.
     *
     * @param entry the new entry to add.
     * @return a new {@code CacheMissInfo} with all previous and new entries.
     */
    public CacheMissInfo withNew(Entries entry) {
        List<Entries> allEntries = new ArrayList<>(entries);
        allEntries.add(entry);
        return new CacheMissInfo(allEntries);
    }

    /**
     * Represents a single cache miss event, capturing metadata about the test context.
     *
     * @param timestamp      the time at which the cache miss occurred.
     * @param classes        the list of configuration classes that triggered the cache miss.
     * @param activeProfiles the list of active profiles used during the test run.
     */
    public record Entries(Instant timestamp, List<String> classes, List<String> activeProfiles) {

        /**
         * Constructs an immutable {@code Entries} record.
         *
         * @param timestamp      the cache miss timestamp.
         * @param classes        the list of configuration classes involved.
         * @param activeProfiles the list of active Spring profiles.
         */
        public Entries(Instant timestamp, List<String> classes, List<String> activeProfiles) {
            this.timestamp = timestamp;
            this.classes = List.copyOf(classes);
            this.activeProfiles = List.copyOf(activeProfiles);
        }

        /**
         * Builds an {@code Entries} instance from a {@link MergedContextConfiguration}.
         *
         * @param config the test context configuration that caused the cache miss.
         * @return a new {@code Entries} instance.
         */
        public static Entries fromConfig(MergedContextConfiguration config) {
            return new Entries(
                    Instant.now(),
                    Arrays.stream(config.getClasses()).map(Class::toString).toList(),
                    Arrays.asList(config.getActiveProfiles())
            );
        }

        @Override
        public List<String> classes() {
            return List.copyOf(classes);
        }

        @Override
        public List<String> activeProfiles() {
            return List.copyOf(activeProfiles);
        }
    }

}
