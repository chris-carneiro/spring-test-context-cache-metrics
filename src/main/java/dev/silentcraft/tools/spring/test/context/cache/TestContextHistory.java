package dev.silentcraft.tools.spring.test.context.cache;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.test.context.MergedContextConfiguration;

/**
 * Holds the complete history of context loading events for a given test class.
 * <p>
 * This record is used to store one or more {@link Events} corresponding to
 * each observed context load event. It is immutable, with each new event generating
 * a new instance via {@link #withNew(Events)} making this class Thread-safe.
 *
 * @param events the list of recorded context cache load events.
 */
public record TestContextHistory(List<Events> events) {

    /**
     * Creates a new immutable {@code TestContextHistory} instance.
     *
     * @param events the list of cache load events.
     */
    public TestContextHistory(List<Events> events) {
        this.events = List.copyOf(events);
    }

    /**
     * Creates a new {@code TestContextHistory} with the first recorded event.
     *
     * @param events the initial event.
     * @return a new {@code TestContextHistory} instance.
     */
    public static TestContextHistory withFirst(Events events) {
        return new TestContextHistory(List.of(events));
    }

    /**
     * Returns an immutable copy of all context load events recorded for this test class.
     *
     * @return an immutable list of events, in the order they were recorded
     */
    @Override
    public List<Events> events() {
        return List.copyOf(events);
    }

    /**
     * Returns all {@link EventType#REBUILD} events in this history.
     * <p>
     * Each entry represents a context that was discarded and rebuilt from scratch
     * after the initial build already existed.
     *
     * @return an immutable list of rebuild events, possibly empty
     */
    public List<Events> rebuildEvents() {
        return events()
                .stream()
                .filter(event -> event.type() == EventType.REBUILD)
                .toList();
    }

    /**
     * Returns all {@link EventType#BUILD} events in this history.
     * <p>
     * Under normal circumstances there is at most one {@code BUILD} event per test run —
     * the very first context loaded across the entire build execution.
     *
     * @return an immutable list of initial build events, possibly empty
     */
    public List<Events> initialBuildEvents() {
        return events().stream()
                .filter(event -> event.type() == EventType.BUILD)
                .toList();
    }


    /**
     * Returns a new {@code TestContextHistory} instance with an additional event
     * making the recording of new events thread-safe for concurrent access.
     *
     * @param event the new event to add.
     * @return a new {@code TestContextHistory} with all previous and new events.
     */
    public TestContextHistory withNew(Events event) {
        List<Events> allEvents = new ArrayList<>(events);
        allEvents.add(event);
        return new TestContextHistory(allEvents);
    }

    /**
     * Returns {@code true} if this context was loaded more than once during
     * the test suite execution, meaning Spring rebuilt the {@code ApplicationContext}
     * from scratch more than once for this configuration.
     *
     * @return {@code true} if the context was reloaded at least once, {@code false} if loaded exactly once.
     */
    public boolean triggeredContextRebuild() {
        return events.stream()
                .anyMatch(event -> event.type() == EventType.REBUILD);
    }

    /**
     * Returns the number of {@link EventType#REBUILD} events in this history.
     *
     * @return the count of rebuild events
     */
    public long rebuildEventsCount() {
        return rebuildEvents().size();
    }


    /**
     * Represents a single context load event captured during test execution.
     *
     * @param type           the {@link EventType} of this event
     * @param timestamp      the time at which the event occurred
     * @param classes        the Spring configuration classes associated with the context
     * @param activeProfiles the active profiles in effect when the event occurred
     */
    public record Events(EventType type, Instant timestamp, List<String> classes, List<String> activeProfiles) {

        /**
         * Constructs an immutable {@code Events} record.
         *
         * @param type           the {@link EventType} of this event
         * @param timestamp      the time at which the event occurred
         * @param classes        the Spring configuration classes associated with the context
         * @param activeProfiles the active profiles in effect when the event occurred
         */
        public Events(EventType type, Instant timestamp, List<String> classes, List<String> activeProfiles) {
            this.type = type;
            this.timestamp = timestamp;
            this.classes = List.copyOf(classes);
            this.activeProfiles = List.copyOf(activeProfiles);
        }

        /**
         * Creates a {@link EventType#REBUILD} event from a cache miss configuration.
         *
         * @param config the merged configuration that triggered the cache miss
         * @return a new {@code Events} instance of type {@code REBUILD}
         */
        public static Events newMiss(MergedContextConfiguration config) {
            return newEvent(EventType.REBUILD, config);
        }

        /**
         * Creates a {@link EventType#REUSE} event from a cache hit configuration.
         *
         * @param config the merged configuration that produced the cache hit
         * @return a new {@code Events} instance of type {@code REUSE}
         */
        public static Events newHit(MergedContextConfiguration config) {
            return newEvent(EventType.REUSE, config);
        }

        /**
         * Creates an {@code Events} instance of the given type from a merged configuration.
         *
         * @param eventType the type to assign to this event
         * @param config    the merged configuration to extract metadata from
         * @return a new {@code Events} instance
         */
        public static Events newEvent(EventType eventType, MergedContextConfiguration config) {
            return new Events(
                    eventType, Instant.now(),
                    Arrays.stream(config.getClasses()).map(Class::toString).toList(),
                    Arrays.stream(config.getActiveProfiles())
                            .filter(profile -> !profile.isBlank())
                            .sorted()
                            .toList()
            );
        }

        /**
         * Creates a {@link EventType#BUILD} event representing the first context load in the build.
         *
         * @param config the merged configuration that triggered the initial build
         * @return a new {@code Events} instance of type {@code BUILD}
         */
        public static Events buildInitial(MergedContextConfiguration config) {
            return newEvent(EventType.BUILD, config);
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
