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

public final class ObservableContextCache implements ContextCache {

    private static final Logger log = LoggerFactory.getLogger(ObservableContextCache.class);

    private final ContextCache delegate = new DefaultContextCache();
    private final Set<ContextCacheMissesListener> listeners = new CopyOnWriteArraySet<>();

    public ObservableContextCache() {
        log.debug("[OCC] New observableContextCache Created");
    }

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
