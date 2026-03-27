package dev.silentcraft.tools.spring.test.context.cache;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.cache.ContextCache;

class ObservableContextCacheTest {

    @Test
    void get_triggersOnCacheHit_whenApplicationContextExists() {
        // GIVEN
        FakeContextCache fakeContextCache = new FakeContextCache();
        FakeMergedContextConfiguration contextConfiguration = new FakeMergedContextConfiguration();
        fakeContextCache.put(contextConfiguration, new GenericApplicationContext());
        ObservableContextCache cache = new ObservableContextCache(fakeContextCache);
        SpyContextCacheListener spyContextCacheListener = new SpyContextCacheListener();
        cache.registerListener(spyContextCacheListener);
        // WHEN
        cache.get(contextConfiguration);


        // THEN
        Assertions.assertEquals(1, spyContextCacheListener.cacheHitCount());
    }

    @Test
    void get_triggersOnCacheMiss_whenApplicationContextNotExists() {
        // GIVEN
        FakeContextCache fakeContextCache = new FakeContextCache();
        ObservableContextCache cache = new ObservableContextCache(fakeContextCache);
        SpyContextCacheListener spyContextCacheListener = new SpyContextCacheListener();
        fakeContextCache.put(new FakeMergedContextConfiguration("unit-test"), new GenericApplicationContext());

        cache.registerListener(spyContextCacheListener);
        // WHEN
        cache.get(new FakeMergedContextConfiguration());


        // THEN
        Assertions.assertEquals(1, spyContextCacheListener.cacheMissesCount());
    }


    private static class SpyContextCacheListener implements ContextCacheMissesListener {
        private final List<Integer> cacheHitCount = new CopyOnWriteArrayList<>();
        private final List<Integer> cacheMissesCount = new CopyOnWriteArrayList<>();

        @Override
        public void onCacheMiss(MergedContextConfiguration key) {
            cacheMissesCount.add(cacheMissesCount.size() + 1);
        }

        @Override
        public void onCacheHit(MergedContextConfiguration key) {
            cacheHitCount.add(cacheHitCount.size() + 1);
        }

        public Integer cacheHitCount() {
            return cacheHitCount.size();
        }

        public Integer cacheMissesCount() {
            return cacheMissesCount.size();
        }
    }

    private static class FakeContextCache implements ContextCache {
        private final Map<MergedContextConfiguration, ApplicationContext> fakeContextMap = new ConcurrentHashMap<>();

        @Override
        public boolean contains(MergedContextConfiguration key) {
            return false;
        }

        @Override
        public ApplicationContext get(MergedContextConfiguration key) {
            return fakeContextMap.get(key);
        }

        @Override
        public void put(MergedContextConfiguration key, ApplicationContext context) {
            fakeContextMap.put(key, context);
        }

        @Override
        public void remove(MergedContextConfiguration key, DirtiesContext.HierarchyMode hierarchyMode) {

        }

        @Override
        public int getFailureCount(MergedContextConfiguration key) {
            return ContextCache.super.getFailureCount(key);
        }

        @Override
        public void incrementFailureCount(MergedContextConfiguration key) {
            ContextCache.super.incrementFailureCount(key);
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public int getParentContextCount() {
            return 0;
        }

        @Override
        public int getHitCount() {
            return 0;
        }

        @Override
        public int getMissCount() {
            return 0;
        }

        @Override
        public void reset() {

        }

        @Override
        public void clear() {

        }

        @Override
        public void clearStatistics() {

        }

        @Override
        public void logStatistics() {

        }
    }

    private static class FakeMergedContextConfiguration extends MergedContextConfiguration {
        @Serial
        private static final long serialVersionUID = 1L;

        FakeMergedContextConfiguration() {
            super(null, null, null, null, null);
        }

        public FakeMergedContextConfiguration(String... activeProfiles) {
            super(null, null, null, activeProfiles, null);
        }

        @Override
        public Class<?> getTestClass() {
            return ObservableContextCacheTest.class;
        }

        @Override
        public Class<?>[] getClasses() {
            return new Class<?>[0];
        }

        @Override
        public String[] getActiveProfiles() {
            throw new UnsupportedOperationException("Fake getActiveProfiles not implemented yet");
        }
    }
}