package dev.silentcraft.tools.junit.execution.listener;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTestBootstrapper;
import dev.silentcraft.tools.spring.test.context.cache.ContextCacheMetricsRegistry;
import dev.silentcraft.tools.spring.test.context.cache.EventType;
import dev.silentcraft.tools.spring.test.context.cache.TestContextHistory;
import dev.silentcraft.tools.spring.test.context.cache.TestContextKey;

/**
 * Global analyzer that hooks into the JUnit Platform's {@link org.junit.platform.launcher.TestExecutionListener}
 * lifecycle to report context cache metrics collected during the test suite execution.
 *
 * <p>
 * This listener automatically activates when the {@link dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTest}
 * annotation is used in any test class in place of Spring boot's {@code @SpringBootTest}. No manual registration or SPI configuration is required.
 *
 * <p>
 * It provides a global analysis of Spring {@link org.springframework.test.context.cache.ContextCache} usage by
 * inspecting the data recorded in the {@link ContextCacheMetricsRegistry}, populated during test bootstrap
 * by {@link dev.silentcraft.tools.spring.test.context.cache.DefaultContextCacheMissesListener}.
 *
 * <h2>What it reports</h2>
 * At the end of the test suite, this listener logs one of two outcomes:
 * <ul>
 *     <li><b>Perfect</b> — no {@link dev.silentcraft.tools.spring.test.context.cache.EventType#REBUILD} events
 *     detected. Every test class reused the same {@code ApplicationContext}.</li>
 *     <li><b>Rebuild report</b> — at least one context rebuild was detected. The report includes:
 *         <ul>
 *             <li>Total number of test classes that triggered a rebuild.</li>
 *             <li>The top 5 offenders by rebuild count, each with their active profiles.</li>
 *             <li>The initial build configuration (class and profiles) that other classes
 *             should align with to achieve context reuse.</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * <h2>Design Notes</h2>
 * This implementation is intentionally internal and does not yet provide public extension points.
 * It demonstrates the potential of analyzing Spring test performance at the suite level.
 *
 * @see ContextCacheMetricsRegistry
 * @see TestContextHistory
 * @see dev.silentcraft.tools.spring.test.context.cache.ObservableContextCache
 * @see dev.silentcraft.tools.spring.test.context.cache.DefaultContextCacheMissesListener
 * @see dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTest
 */
public class GlobalTestExecutionAnalyzer implements TestExecutionListener {
    private static final Logger log = LoggerFactory.getLogger(GlobalTestExecutionAnalyzer.class);
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_COLOR_END = "\u001B[0m";

    /**
     * Creates a new {@code GlobalTestExecutionAnalyzer}.
     * Instantiated by the JUnit Platform via the {@link java.util.ServiceLoader} SPI.
     */
    public GlobalTestExecutionAnalyzer() {
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        log.info("TestPlan Execution started!");
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        log.info("TestPlan Execution finished!");
        if (!CacheAwareSpringBootTestBootstrapper.isActivated()) {
            return;
        }
        analyzeResults();
    }

    private void analyzeResults() {
        Map<TestContextKey, TestContextHistory> snapshot = ContextCacheMetricsRegistry.snapshot();

        if (contextWasBuiltOnlyOnce(snapshot)) {
            log.info("[OCC] {} Perfect! No cache misses detected, all your tests share the same configuration. {}", ANSI_YELLOW, ANSI_COLOR_END);
            return;
        }

        log.warn("[OCC] {} Cache Miss Analysis: {}", ANSI_YELLOW, ANSI_COLOR_END);
        log.warn("[OCC] {} Total cache misses detected: {} {} - following the {} 5 most impactful {} test classes", ANSI_YELLOW, snapshot.values().stream().
                filter(TestContextHistory::triggeredContextRebuild).count(), ANSI_COLOR_END, ANSI_YELLOW, ANSI_COLOR_END);


        // Top offenders
        snapshot.entrySet()
                .stream()
                .filter(e -> e.getValue().triggeredContextRebuild())
                .sorted((e1, e2) -> Long.compare(
                        e2.getValue().rebuildEventsCount(),
                        e1.getValue().rebuildEventsCount()))
                .limit(5)
                .forEach(entry -> {
                    TestContextKey key = entry.getKey();
                    log.warn("[OCC] {} /!\\ {} {} - Could not reuse cached application context ", ANSI_YELLOW,
                            key.testClass().getSimpleName(), ANSI_COLOR_END);
                    log.warn("[OCC] {} {} - Active profiles {} {}", ANSI_YELLOW,
                            key.testClass().getSimpleName(), collectRebuildEventsActiveProfiles(entry), ANSI_COLOR_END);
                });

        log.warn("[OCC] - {} Cached application context {} was based on this (class) & [configuration] {}  {} {} " +
                "- use it to configure test classes that could not use cached context.", ANSI_YELLOW, ANSI_COLOR_END, ANSI_YELLOW, collectInitialBuildProperties(snapshot), ANSI_COLOR_END);


    }

    private static String collectRebuildEventsActiveProfiles(Map.Entry<TestContextKey, TestContextHistory> entry) {
        return entry.getValue().rebuildEvents()
                .stream()
                .flatMap(events -> events.activeProfiles().stream())
                .collect(Collectors.collectingAndThen(Collectors.joining(",", "[", "]"),
                        pr -> {
                            if (pr.isBlank()) {
                                return "No Active profiles";
                            }
                            return pr;
                        })
                );
    }

    private static String collectInitialBuildProperties(Map<TestContextKey, TestContextHistory> snapshot) {
        return snapshot.entrySet().stream()
                .filter(entry -> {
                    return entry.getValue().events().stream()
                            .anyMatch(event -> event.type() == EventType.BUILD);
                })
                .map(entry -> {
                    TestContextKey key = entry.getKey();
                    return entry.getValue().initialBuildEvents().stream()
                            .map(TestContextHistory.Events::activeProfiles)
                            .flatMap(List::stream)
                            .collect(Collectors.collectingAndThen(Collectors.joining(",", "[", "]"),
                                    activeProfiles -> {
                                        String testClass = key.testClass().getSimpleName();
                                        if (activeProfiles.isBlank()) {
                                            return "%s has no Active profiles".formatted(testClass);
                                        }
                                        return "(%s) - {profiles: %s}".formatted(testClass, activeProfiles);
                                    }

                            ));
                }).collect(Collectors.joining("-"));
    }

    private static boolean contextWasBuiltOnlyOnce(Map<TestContextKey, TestContextHistory> snapshot) {
        return snapshot.values().stream()
                .map(TestContextHistory::events)
                .flatMap(List::stream)
                .noneMatch(events -> events.type() == EventType.REBUILD);
    }
}
