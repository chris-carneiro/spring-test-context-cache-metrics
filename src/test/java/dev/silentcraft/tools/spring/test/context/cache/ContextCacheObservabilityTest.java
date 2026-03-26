package dev.silentcraft.tools.spring.test.context.cache;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * End-to-end observability tests verifying that the full pipeline
 * (ObservableContextCache → DefaultContextCacheMissesListener → ContextCacheMetricsRegistry)
 * records cache misses and cache hits correctly.
 *
 * <p>Inner test fixtures annotated with {@code @DirtiesContext(classMode = BEFORE_CLASS)}
 * guarantee a fresh context load on every run, making assertions deterministic
 * regardless of prior JVM cache state.</p>
 */
class ContextCacheObservabilityTest {

    @BeforeEach
    void clearRegistry() {
        ContextCacheMetricsRegistry.clear();
    }

    @Test
    void missIsRecordedWhenContextIsLoaded() {
        run(AlphaTest.class);

        Map<CacheMissInfoKey, CacheMissInfo> snapshot = ContextCacheMetricsRegistry.snapshot();

        Assertions.assertTrue(
                snapshot.keySet().stream().anyMatch(k -> k.testClass() == AlphaTest.class),
                "A cache miss must be recorded when a context is loaded for the first time"
        );
    }

    @Test
    void twoDistinctConfigsEachRecordAMiss() {
        run(BetaZooTest.class, GammaBeachTest.class);

        Map<CacheMissInfoKey, CacheMissInfo> snapshot = ContextCacheMetricsRegistry.snapshot();

        Assertions.assertTrue(
                snapshot.keySet().stream().anyMatch(k -> k.testClass() == BetaZooTest.class),
                "Cache miss must be recorded for BetaZooTest (profile: zoo)"
        );
        Assertions.assertTrue(
                snapshot.keySet().stream().anyMatch(k -> k.testClass() == GammaBeachTest.class),
                "Cache miss must be recorded for GammaBeachTest (profile: beach)"
        );
    }

    @Test
    void identicalConfigCausesOnlyOneMiss() {
        // DeltaTest (BEFORE_CLASS) forces a fresh load → miss recorded, context placed in cache.
        // EpsilonTest has an identical MergedContextConfiguration and no DirtiesContext,
        // so it reuses the cached context → hit → no entry in registry.
        run(DeltaTest.class);
        run(EpsilonTest.class);

        Map<CacheMissInfoKey, CacheMissInfo> snapshot = ContextCacheMetricsRegistry.snapshot();

        Assertions.assertFalse(
                snapshot.containsKey(new CacheMissInfoKey(EpsilonTest.class)),
                "EpsilonTest must not appear in the registry — it reused DeltaTest's cached context"
        );
    }

    // --- helper ---

    private static void run(Class<?>... classes) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(Arrays.stream(classes)
                        .map(DiscoverySelectors::selectClass)
                        .toList())
                .build();
        LauncherFactory.create().execute(request);
    }

    // --- inner test fixtures ---

    /**
     * No-profile fixture. BEFORE_CLASS guarantees a cache miss on every execution.
     */
    @CacheAwareSpringBootTest
    @DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
    static class AlphaTest {
        @Autowired ApplicationContext ctx;
        @Test void runs() { Assertions.assertNotNull(ctx); }
    }

    /**
     * Profile "zoo" fixture. BEFORE_CLASS guarantees a cache miss on every execution.
     */
    @ActiveProfiles("zoo")
    @CacheAwareSpringBootTest
    @DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
    static class BetaZooTest {
        @Autowired ApplicationContext ctx;
        @Test void runs() { Assertions.assertNotNull(ctx); }
    }

    /**
     * Profile "beach" fixture. BEFORE_CLASS guarantees a cache miss on every execution.
     */
    @ActiveProfiles("beach")
    @CacheAwareSpringBootTest
    @DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
    static class GammaBeachTest {
        @Autowired ApplicationContext ctx;
        @Test void runs() { Assertions.assertNotNull(ctx); }
    }

    /**
     * No-profile fixture used to seed the cache. BEFORE_CLASS guarantees a miss,
     * leaving a fresh context in the cache for EpsilonTest to reuse.
     */
    @CacheAwareSpringBootTest
    @DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
    static class DeltaTest {
        @Autowired ApplicationContext ctx;
        @Test void runs() { Assertions.assertNotNull(ctx); }
    }

    /**
     * No-profile fixture identical in configuration to DeltaTest. No DirtiesContext —
     * it must reuse the context loaded by DeltaTest (cache hit, not a miss).
     */
    @CacheAwareSpringBootTest
    static class EpsilonTest {
        @Autowired ApplicationContext ctx;
        @Test void runs() { Assertions.assertNotNull(ctx); }
    }
}
