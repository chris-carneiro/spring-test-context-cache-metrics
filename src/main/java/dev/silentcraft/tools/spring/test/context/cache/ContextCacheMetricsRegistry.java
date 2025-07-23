package dev.silentcraft.tools.spring.test.context.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * Registry for tracking cache miss events in Spring's {@link org.springframework.test.context.cache.ContextCache}.
 * <p>
 * This class maintains a thread-safe internal registry of cache miss information,
 * keyed by test class, allowing consumers to monitor how often and under what
 * conditions Spring test application contexts are not reused.
 * <p>
 * It is primarily used in conjunction with {@link ObservableContextCache} and
 * {@link ContextCacheMissesListener} implementations to analyze and optimize
 * test suite performance by identifying duplicate or suboptimally configured test contexts.
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * ContextCacheMetricsRegistry registry = new ContextCacheMetricsRegistry();
 * registry.recordEntry(config);
 *
 * Map<CacheMissInfoKey, CacheMissInfo> snapshot = ContextCacheMetricsRegistry.snapshot();
 * snapshot.forEach((key, info) -> System.out.println(info));
 * }</pre>
 *
 * <h3>Thread Safety</h3>
 * Internally uses a {@link ConcurrentHashMap}, making it safe for use in parallel test executions.
 *
 * @see ObservableContextCache
 * @see ContextCacheMissesListener
 * @see CacheMissInfo
 * @see CacheMissInfoKey
 */
public class ContextCacheMetricsRegistry {
    private static final Logger log = LoggerFactory.getLogger(ContextCacheMetricsRegistry.class);

    private static final Map<CacheMissInfoKey, CacheMissInfo> CACHE_MISS_INFO_METRICS = new ConcurrentHashMap<>();

    /**
     * Records a new context cache miss entry for the given merged configuration.
     * <p>
     * If an entry already exists for the test class, the new miss is added to the existing
     * {@link CacheMissInfo}. Otherwise, a new entry is initialized.
     *
     * @param config the merged test configuration that triggered a cache miss
     */
    public void recordEntry(MergedContextConfiguration config) {
        CacheMissInfoKey key = new CacheMissInfoKey(config.getTestClass());
        CacheMissInfo.Entries entry = CacheMissInfo.Entries.fromConfig(config);

        CACHE_MISS_INFO_METRICS.compute(key, (testClass, info) -> {
            if (info == null) {
                return CacheMissInfo.withFirst(entry);
            }
            return info.withNew(entry);
        });

        log.debug("[OCC] Cache miss recorded for {}", key);
    }

    /**
     * Returns an immutable snapshot of the currently recorded cache miss metrics.
     * <p>
     * Useful for exporting, analyzing, or logging after the test suite completes.
     *
     * @return a read-only view of the current cache miss registry
     */
    public static Map<CacheMissInfoKey, CacheMissInfo> snapshot() {
        if (CACHE_MISS_INFO_METRICS.isEmpty()) {
            log.warn("No cache miss info found - did you forget to call ContextCacheMetricsRegistry#recordEntry?");
        }
        return Map.copyOf(CACHE_MISS_INFO_METRICS);
    }
}
