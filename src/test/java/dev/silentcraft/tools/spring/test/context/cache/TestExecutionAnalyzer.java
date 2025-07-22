package dev.silentcraft.tools.spring.test.context.cache;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestExecutionListener;
@Component
public class TestExecutionAnalyzer implements TestExecutionListener {
    private static final Logger log = LoggerFactory.getLogger(TestExecutionAnalyzer.class);

    @EventListener
    @Async
    public void onApplicationEvent(ContextClosedEvent event) {
        analyzeResults();
    }

    private void analyzeResults() {
        Map<CacheMissInfoKey, CacheMissInfo> snapshot = ContextCacheMetricsRegistry.snapshot();

        if (snapshot.isEmpty()) {
            log.info("[OCC] ✅ Perfect! No cache misses detected");
            return;
        }

        log.warn("[OCC] 📊 Cache Miss Analysis:");
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
                log.warn("[OCC] 🔴 {} - {} cache misses",
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

        log.info("[OCC] 💡 Most common profiles: {}",
            profilePatterns.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue)));
    }
}
