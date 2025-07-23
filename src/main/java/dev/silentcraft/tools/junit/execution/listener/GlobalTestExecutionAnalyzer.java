package dev.silentcraft.tools.junit.execution.listener;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.silentcraft.tools.spring.test.context.cache.CacheMissInfo;
import dev.silentcraft.tools.spring.test.context.cache.CacheMissInfoKey;
import dev.silentcraft.tools.spring.test.context.cache.ContextCacheMetricsRegistry;

/**
 * Global analyzer that hooks into the JUnit Platform's {@link org.junit.platform.launcher.TestExecutionListener}
 * lifecycle to report context cache metrics collected during the test suite execution.
 *
 * <p>
 * This listener automatically activates when the {@link dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTest}
 * annotation is used in any test class in place of Spring boot's {@code @SpringBootTest}. No manual registration or SPI configuration is required.
 *
 * <p>
 * It provides a global analysis of Spring {@link org.springframework.test.context.cache.ContextCache} misses by
 * inspecting the data recorded in the {@link ContextCacheMetricsRegistry}, typically populated during test bootstrap
 * by {@link dev.silentcraft.tools.spring.test.context.cache.DefaultContextCacheMissesListener}.
 *
 * <h3>What it reports</h3>
 * At the end of the test suite, this listener logs:
 * <ul>
 *     <li>A success message if no context cache misses were detected.</li>
 *     <li>A ranked summary of the top test classes responsible for cache misses.</li>
 *     <li>A list of the most frequently used Spring profiles across miss events.</li>
 * </ul>
 *
 * <h3>Design Notes</h3>
 * This implementation is intentionally internal and does not yet provide public extension points.
 * It demonstrates the potential of analyzing Spring test performance at the suite level.

 * <h3>Example Output</h3>
 * <pre>{@code
 * TestPlan Exceution finished!!
 * Cache Miss Analysis:
 * MyControllerTest - 3 cache misses
 * UserApiTest - 2 cache misses
 * Most common profiles: {test=5, integration=2}
 * }</pre>
 *
 * @see ContextCacheMetricsRegistry
 * @see CacheMissInfo
 * @see dev.silentcraft.tools.spring.test.context.cache.ObservableContextCache
 * @see dev.silentcraft.tools.spring.test.context.cache.DefaultContextCacheMissesListener
 * @see dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTest
 */
public class GlobalTestExecutionAnalyzer implements TestExecutionListener {
    private static final Logger log = LoggerFactory.getLogger(GlobalTestExecutionAnalyzer.class);


    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        log.info("TestPlan Execution finished!");
        analyzeResults();
    }

    private void analyzeResults() {
        Map<CacheMissInfoKey, CacheMissInfo> snapshot = ContextCacheMetricsRegistry.snapshot();

        if (snapshot.isEmpty()) {
            log.info("[OCC] Perfect! No cache misses detected");
            return;
        }

        log.warn("[OCC] Cache Miss Analysis:");
        log.warn("[OCC] Total test classes with cache misses: {}", snapshot.size());

        // Top offenders
        snapshot.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(
                        e2.getValue().entries().size(),
                        e1.getValue().entries().size()))
                .limit(5)
                .forEach(entry -> {
                    CacheMissInfoKey key = entry.getKey();
                    CacheMissInfo info = entry.getValue();
                    log.warn("[OCC] /!\\ {} - {} cache misses",
                            key.testClass().getSimpleName(),
                            info.entries().size());
                });

        // Suggestions
        suggestOptimizations(snapshot);


    }

    private void suggestOptimizations(Map<CacheMissInfoKey, CacheMissInfo> snapshot) {
        // Detect common patterns
        Map<String, Long> profilePatterns = snapshot.values().stream()
                .flatMap(info -> info.entries().stream())
                .flatMap(entry -> entry.activeProfiles().stream())
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()));

        log.info("[OCC] Most common profiles: {}",
                profilePatterns.entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .limit(3)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue)));
    }
}
