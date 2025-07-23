package dev.silentcraft.tools.spring.test.context.cache;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.cache.ContextCache;
import org.springframework.test.context.cache.DefaultContextCache;

/**
 * A {@link ContextCache} implementation that wraps the default {@link DefaultContextCache}
 * and provides observability on context cache misses.
 *
 * <p>This class is intended for use in test instrumentation and diagnostics,
 * especially when analyzing {@code ApplicationContext} reuse during test execution.
 * It enables external listeners to be notified whenever a context miss occurs —
 * i.e., when a requested {@code MergedContextConfiguration} does not exist in the cache.</p>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Track context reloads that could indicate improper test isolation.</li>
 *   <li>Provide metrics or logs about unnecessary context recreation.</li>
 *   <li>Diagnose test performance regressions caused by context fragmentation.</li>
 * </ul>
 *
 * <h2>Listener Registration</h2>
 * Listeners can be registered at runtime using {@link #registerListener(ContextCacheMissesListener)}.
 * Each registered {@link ContextCacheMissesListener} will be notified with the
 * {@link MergedContextConfiguration} that triggered the miss:
 *
 * <pre>{@code
 * observableContextCache.registerListener(ctx -> {
 *     log.warn("Context miss for: {}", ctx);
 * });
 * }</pre>
 *
 * <h2>Spring Integration</h2>
 * This class is designed to be injected into Spring's testing infrastructure via
 * a custom {@link org.springframework.test.context.CacheAwareContextLoaderDelegate}, like so:
 *
 * <pre>{@code
 * @Override
 * public CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate() {
 *     return new DefaultCacheAwareContextLoaderDelegate(observableContextCache);
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * Listener registration is thread-safe thanks to the use of a {@link CopyOnWriteArraySet}.
 *
 * @see org.springframework.test.context.cache.ContextCache
 * @see DefaultContextCache
 * @see ContextCacheMissesListener
 * @see dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTestBootstrapper
 */
public final class ObservableContextCache implements ContextCache {

    private static final Logger log = LoggerFactory.getLogger(ObservableContextCache.class);

    private final ContextCache delegate;
    private final Set<ContextCacheMissesListener> listeners = new CopyOnWriteArraySet<>();

    /**
     * Constructs an {@code ObservableContextCache} that wraps the given {@link ContextCache} delegate.
     *
     * @param delegate the underlying {@code ContextCache} to decorate and observe
     */
    public ObservableContextCache(ContextCache delegate) {
        this.delegate = delegate;
        log.debug("[OCC] New observableContextCache Created");
    }

    /**
     * Constructs an {@code ObservableContextCache} with a default {@link DefaultContextCache} delegate.
     */
    public ObservableContextCache() {
        this(new DefaultContextCache());
    }

    /**
     * Registers a listener that will be notified when a context cache miss occurs.
     * <p>
     * All registered {@link ContextCacheMissesListener}s will receive callbacks
     * whenever {@link #get(MergedContextConfiguration)} results in a miss (i.e., the context is not found).
     * <p>
     * This method is thread-safe and allows registering listeners at runtime.
     *
     * @param listener the listener to register (must not be {@code null})
     * @see ContextCacheMissesListener#onCacheMiss(MergedContextConfiguration)
     */
    public void registerListener(ContextCacheMissesListener listener) {
        listeners.add(listener);
    }

    @Override
    public boolean contains(MergedContextConfiguration key) {
        return delegate.contains(key);
    }

    @Override
    public ApplicationContext get(MergedContextConfiguration contextKey) {
        ApplicationContext applicationContext = delegate.get(contextKey);
        if (applicationContext == null) {
            listeners.forEach(listener -> listener.onCacheMiss(contextKey));
        }

        return applicationContext;
    }

    @Override
    public void put(MergedContextConfiguration key, ApplicationContext context) {
        delegate.put(key, context);
    }

    @Override
    public void remove(MergedContextConfiguration key, DirtiesContext.HierarchyMode hierarchyMode) {
        delegate.remove(key, hierarchyMode);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public int getParentContextCount() {
        return delegate.getParentContextCount();
    }

    @Override
    public int getHitCount() {
        return delegate.getHitCount();
    }

    @Override
    public int getMissCount() {
        return delegate.getMissCount();
    }

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public void clearStatistics() {
        delegate.clearStatistics();
    }

    @Override
    public void logStatistics() {
        delegate.logStatistics();
    }
}
