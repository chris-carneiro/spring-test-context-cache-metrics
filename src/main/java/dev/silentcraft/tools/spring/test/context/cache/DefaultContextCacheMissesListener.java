package dev.silentcraft.tools.spring.test.context.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * Default implementation of {@link ContextCacheMissesListener} that records context cache hits
 * and misses into {@link ContextCacheMetricsRegistry}.
 * <p>
 * This implementation is registered statically by {@link CacheAwareSpringBootTestBootstrapper}
 * on the shared {@link ObservableContextCache}, ensuring all cache events are captured before
 * any {@code ApplicationContext} is loaded.
 * <p>
 * The recorded data includes the test class, involved configuration classes, active profiles,
 * and the timestamp of each event.
 *
 * <h2>Metrics Access</h2>
 * Collected metrics are accessible after the test suite via:
 * {@link ContextCacheMetricsRegistry#snapshot()}.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Map<TestContextKey, TestContextHistory> snapshot = ContextCacheMetricsRegistry.snapshot();
 * snapshot.forEach((key, info) -> System.out.println(info));
 * }</pre>
 *
 * @see ContextCacheMetricsRegistry
 * @see ObservableContextCache
 * @see ContextCacheMissesListener
 */
public class DefaultContextCacheMissesListener implements ContextCacheMissesListener {
    private static final Logger log = LoggerFactory.getLogger(DefaultContextCacheMissesListener.class);

    /**
     * Creates a new {@code DefaultContextCacheMissesListener}.
     * Instantiated internally by {@link CacheAwareSpringBootTestBootstrapper}.
     */
    public DefaultContextCacheMissesListener() {
    }

    @Override
    public void onCacheMiss(MergedContextConfiguration miss) {
        ContextCacheMetricsRegistry.recordMiss(miss);
    }

    @Override
    public void onCacheHit(MergedContextConfiguration hit) {
        ContextCacheMetricsRegistry.recordHit(hit);
    }
}
