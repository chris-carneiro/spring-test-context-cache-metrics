package dev.silentcraft.tools.junit.execution.listener;

import java.lang.reflect.Field;
import java.util.Map;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTestBootstrapper;
import dev.silentcraft.tools.spring.test.context.cache.ContextCacheMetricsRegistry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalTestExecutionAnalyzerTest {

    private final GlobalTestExecutionAnalyzer analyzer = new GlobalTestExecutionAnalyzer();

    private final Logger analyzerLogger =
            (Logger) LoggerFactory.getLogger(GlobalTestExecutionAnalyzer.class);

    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

    @BeforeEach
    void setUp() throws Exception {
        clearRegistry();
        logAppender.start();
        analyzerLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() throws Exception {
        analyzerLogger.detachAppender(logAppender);
        logAppender.stop();
        resetActivated();
    }

    @Test
    void shouldBeSilentWhenBootstrapperWasNeverActivated() throws Exception {
        setActivated(false);

        analyzer.testPlanExecutionFinished(null);

        boolean occLogged = logAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains("[OCC]"));

        assertFalse(occLogged,
                "Analyzer must produce no [OCC] output when @CacheAwareSpringBootTest was never used");
    }

    @Test
    void shouldReportSuccessWhenActivatedWithNoMisses() throws Exception {
        setActivated(true);

        analyzer.testPlanExecutionFinished(null);

        boolean successLogged = logAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains("[OCC] Perfect!"));

        assertTrue(successLogged,
                "Analyzer must report success when bootstrapper was active but no cache misses occurred");
    }

    private static void setActivated(boolean value) throws Exception {
        Field field = CacheAwareSpringBootTestBootstrapper.class.getDeclaredField("activated");
        field.setAccessible(true);
        field.set(null, value);
    }

    private static void resetActivated() throws Exception {
        setActivated(false);
    }

    private static void clearRegistry() throws Exception {
        Field field = ContextCacheMetricsRegistry.class.getDeclaredField("CACHE_MISS_INFO_METRICS");
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).clear();
    }
}
