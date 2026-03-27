package dev.silentcraft.tools.junit.execution.listener;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

import dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTest;
import dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTestBootstrapper;
import dev.silentcraft.tools.spring.test.context.cache.TestContextHistory;
import dev.silentcraft.tools.spring.test.context.cache.TestContextKey;
import dev.silentcraft.tools.spring.test.context.cache.ContextCacheMetricsRegistry;
import dev.silentcraft.tools.spring.test.context.cache.TestApplication;

/**
 * Verifies that the full observability pipeline is wired correctly when tests run
 * through the JUnit Platform Launcher.
 *
 * <p>Log output produced by {@link GlobalTestExecutionAnalyzer} is not asserted here
 * because Spring Boot's {@code LogbackLoggingSystem} resets the Logback context during
 * application context initialization, stripping any programmatically added appenders
 * before the analyzer fires. Log output behaviour is covered in isolation by
 * {@link GlobalTestExecutionAnalyzerTest}.</p>
 *
 * <p>These tests instead assert on {@link ContextCacheMetricsRegistry} — the registry
 * that the analyzer reads from — which is the reliable observable end of the pipeline.</p>
 */
class GlobalTestExecutionAnalyzerLauncherTest {

    @BeforeEach
    void clearRegistry() {
        ContextCacheMetricsRegistry.clear();
    }

    @Test
    void bootstrapperIsActivatedWhenCacheAwareTestRunsViaLauncher() {
        run(ZooMissTest.class);

        assertTrue(CacheAwareSpringBootTestBootstrapper.isActivated(),
                "Bootstrapper must be marked activated after a @CacheAwareSpringBootTest runs via the Launcher");
    }

    @Test
    void missIsRegisteredInRegistryWhenContextIsLoadedViaLauncher() {
        run(ZooMissTest.class);

        Map<TestContextKey, TestContextHistory> snapshot = ContextCacheMetricsRegistry.snapshot();

        assertTrue(
                snapshot.keySet().stream().anyMatch(k -> k.testClass() == ZooMissTest.class),
                "Registry must contain an entry for ZooMissTest after it causes a cache miss"
        );
    }

    @Test
    void noMissIsRegisteredWhenPlainJUnitTestRunsViaLauncher() {
        run(PlainJUnitFixture.class);

        Map<TestContextKey, TestContextHistory> snapshot = ContextCacheMetricsRegistry.snapshot();

        assertTrue(snapshot.isEmpty(),
                "Registry must remain empty when no @CacheAwareSpringBootTest runs");
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

    // --- fixtures ---

    /**
     * Spring Boot fixture. BEFORE_CLASS guarantees a cache miss on every execution.
     *
     * <p>TestApplication is declared explicitly: this inner class lives in package
     * {@code dev.silentcraft.tools.junit.execution.listener}, outside the branch that
     * contains TestApplication, so Spring Boot's auto-detection would not find it.</p>
     */
    @ActiveProfiles("zoo")
    @CacheAwareSpringBootTest(classes = TestApplication.class)
    @DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
    static class ZooMissTest {
        @Autowired ApplicationContext ctx;
        @Test void runs() { Assertions.assertNotNull(ctx); }
    }

    /**
     * Plain JUnit fixture — no Spring context, no @CacheAwareSpringBootTest.
     * Triggers testPlanExecutionFinished() without touching the registry or the cache.
     */
    static class PlainJUnitFixture {
        @Test void runs() { Assertions.assertTrue(true); }
    }
}
