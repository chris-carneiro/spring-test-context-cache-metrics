package dev.silentcraft.tools.spring.test.context.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * Default implementation of {@link ContextCacheMissesListener} that records all context cache misses
 * into an internal metrics registry, {@link ContextCacheMetricsRegistry}.
 * <p>
 * This implementation is automatically registered by {@link ObservableContextCache} to monitor
 * Spring's {@link org.springframework.test.context.cache.ContextCache} during test execution.
 * <p>
 * The recorded data includes the test class, involved configuration classes, active profiles,
 * and the timestamp of each cache miss.
 *
 * <h3>Metrics Access</h3>
 * The collected metrics are stored in a static instance of {@link ContextCacheMetricsRegistry}
 * and can be accessed after the test suite via the static method:
 * {@link ContextCacheMetricsRegistry#snapshot()}.
 *
 * <h3>Example Usage</h3>
 * <pre>{@code
 * Map<CacheMissInfoKey, CacheMissInfo> snapshot = ContextCacheMetricsRegistry.snapshot();
 * snapshot.forEach((key, info) -> System.out.println(info));
 * }</pre>
 *
 * @see ContextCacheMetricsRegistry
 * @see ObservableContextCache
 * @see ContextCacheMissesListener
 */
public class DefaultContextCacheMissesListener implements ContextCacheMissesListener {
    private static final Logger log = LoggerFactory.getLogger(DefaultContextCacheMissesListener.class);

    private static final ContextCacheMetricsRegistry contextCacheMetricsRegistry = new ContextCacheMetricsRegistry();

    @Override
    public void onCacheMiss(MergedContextConfiguration config) {
        contextCacheMetricsRegistry.recordEntry(config);
    }
}
