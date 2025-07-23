package dev.silentcraft.tools.spring.test.context.cache;

import org.springframework.test.context.MergedContextConfiguration;

/**
 * Listener interface for receiving notifications when a test context is not found in the Spring {@link org.springframework.test.context.cache.ContextCache}.
 * <p>
 * Implementations of this interface can be registered via {@link ObservableContextCache#registerListener(ContextCacheMissesListener)}
 * to track context cache misses during test execution.
 *
 * <h3> Design Limitation</h3>
 * Spring’s {@link org.springframework.test.context.cache.ContextCache} is configured during the early
 * bootstrap phase of test initialization — before the {@link org.springframework.context.ApplicationContext}
 * is created and outside of the Spring dependency injection lifecycle.
 * <p>
 * Therefore, listeners registered dynamically at runtime (e.g., as Spring beans, through {@code @BeforeAll}, or JUnit extensions)
 * will <strong>not</strong> be notified for cache misses occurring during the application context bootstrap.
 * <p>
 * Only statically registered listeners (e.g., via static initialization in a {@code SpringBootTestContextBootstrapper})
 * will reliably observe all cache miss events.
 *
 * <h3>Future-Proofing</h3>
 * The interface remains open for extension in anticipation of future evolutions of the Spring test lifecycle.
 *
 * @see ObservableContextCache
 * @see DefaultContextCacheMissesListener
 * @see CacheAwareSpringBootTestBootstrapper
 */
public interface ContextCacheMissesListener {
    void onCacheMiss(MergedContextConfiguration key);
}
