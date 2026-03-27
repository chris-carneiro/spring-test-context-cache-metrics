package dev.silentcraft.tools.spring.test.context.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * Registry for tracking context cache events in Spring's {@link org.springframework.test.context.cache.ContextCache}.
 * <p>
 * This class maintains a thread-safe internal registry of context load events keyed by test class,
 * allowing consumers to monitor how often and under what conditions Spring rebuilds or reuses
 * application contexts across the test suite.
 * <p>
 * Each event is classified as {@link EventType#BUILD} (first context ever created in the build),
 * {@link EventType#REBUILD} (subsequent context load due to a cache miss), or
 * {@link EventType#REUSE} (cache hit — existing context returned without rebuilding).
 * <p>
 * It is primarily used in conjunction with {@link ObservableContextCache} and
 * {@link ContextCacheMissesListener} implementations to analyze and optimize
 * test suite performance by identifying duplicate or suboptimally configured test contexts.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * Map<TestContextKey, TestContextHistory> snapshot = ContextCacheMetricsRegistry.snapshot();
 * snapshot.forEach((key, history) -> System.out.println(key + ": " + history.events()));
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * Internally uses a {@link ConcurrentHashMap}, making it safe for use in parallel test executions.
 *
 * @see ObservableContextCache
 * @see ContextCacheMissesListener
 * @see TestContextHistory
 * @see TestContextKey
 */
public class ContextCacheMetricsRegistry {
    private static final Logger log = LoggerFactory.getLogger(ContextCacheMetricsRegistry.class);

    private static final Map<TestContextKey, TestContextHistory> CACHE_MISS_INFO_METRICS = new ConcurrentHashMap<>();
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_COLOR_END = "\u001B[0m";

    /**
     * Clears all recorded context cache events from the registry.
     * <p>
     * Useful for per-module isolation in multi-module builds, or to reset state between
     * repeated test runs in an IDE that reuses the JVM.
     */
    public static void clear() {
        log.debug("[OCC] clearing misses records");
        CACHE_MISS_INFO_METRICS.clear();
    }

    private ContextCacheMetricsRegistry() {
    }

    /**
     * Records a context cache miss for the given merged configuration.
     * <p>
     * If the registry is empty this is the first context load in the build — the event is
     * recorded as {@link EventType#BUILD}. Otherwise it is recorded as {@link EventType#REBUILD}.
     * <p>
     * If a history already exists for the test class the new event is appended; otherwise
     * a new history is created.
     *
     * @param context the merged test configuration that triggered the cache miss
     */
    public static void recordMiss(MergedContextConfiguration context) {
        TestContextKey key = new TestContextKey(context.getTestClass());

        if (CACHE_MISS_INFO_METRICS.isEmpty()) {
            log.info("[OCC] {} {} triggered application context cache first build {}", ANSI_GREEN, key.testClass().getSimpleName(), ANSI_COLOR_END);
            TestContextHistory.Events contextBuild = TestContextHistory.Events.buildInitial(context);

            CACHE_MISS_INFO_METRICS.put(key, TestContextHistory.withFirst(contextBuild));
            return;
        }

        CACHE_MISS_INFO_METRICS.compute(key, (testClass, history) -> {
            if (history == null) {
                TestContextHistory.Events contextBuild = TestContextHistory.Events.newMiss(context);
                return TestContextHistory.withFirst(contextBuild);
            }

            TestContextHistory.Events contextRebuild = TestContextHistory.Events.newMiss(context);
            return history.withNew(contextRebuild);
        });

        log.info("[OCC] Cache miss recorded for {}", key.testClass().getSimpleName());
    }


    /**
     * Records a new context cache hit event for the given merged configuration.
     * <p>
     * If an event already exists for the test class, the new hit is added to the existing
     * {@link TestContextHistory}. Otherwise, a new event is initialized.
     *
     * @param config the merged test configuration that triggered a cache hit
     */
    public static void recordHit(MergedContextConfiguration config) {
        TestContextKey key = new TestContextKey(config.getTestClass());
        TestContextHistory.Events event = TestContextHistory.Events.newHit(config);

        CACHE_MISS_INFO_METRICS.compute(key, (testClass, history) -> {
            if (history == null) {
                return TestContextHistory.withFirst(event);
            }
            return history.withNew(event);
        });

        log.debug("[OCC] Cache hit recorded for {}", key);
    }


    /**
     * Returns an immutable snapshot of all context load events recorded so far.
     * <p>
     * Useful for exporting, analyzing, or logging after the test suite completes.
     *
     * @return a read-only view of the current registry
     */
    public static Map<TestContextKey, TestContextHistory> snapshot() {
        if (CACHE_MISS_INFO_METRICS.isEmpty()) {
            log.debug("No cache miss info found - did you forget to call ContextCacheMetricsRegistry#recordMiss?");
        }
        return Map.copyOf(CACHE_MISS_INFO_METRICS);
    }


}
