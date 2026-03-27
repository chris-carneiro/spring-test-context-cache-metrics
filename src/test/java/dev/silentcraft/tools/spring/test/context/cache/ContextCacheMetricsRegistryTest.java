package dev.silentcraft.tools.spring.test.context.cache;

import java.io.Serial;
import java.lang.reflect.Field;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.MergedContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextCacheMetricsRegistryTest {

    private final Logger registryLogger =
            (Logger) LoggerFactory.getLogger(ContextCacheMetricsRegistry.class);

    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

    @BeforeEach
    void setUp() throws Exception {
        clearRegistry();
        logAppender.start();
        registryLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        registryLogger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Test
    void snapshot_shouldNotLogWarnWhenRegistryIsEmpty() {
        ContextCacheMetricsRegistry.snapshot();

        boolean warnLogged = logAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.WARN);

        assertFalse(warnLogged,
                "snapshot() must not log WARN when registry is empty — no cache misses is a success, not an error");
    }

    @Test
    void clear_shouldEmptyRegistry() {
        ContextCacheMetricsRegistry.recordMiss(new FakeMergedContextConfiguration());
        assertFalse(ContextCacheMetricsRegistry.snapshot().isEmpty(), "pre-condition: registry has data");

        ContextCacheMetricsRegistry.clear();

        assertTrue(ContextCacheMetricsRegistry.snapshot().isEmpty(),
                "clear() must empty the registry");
    }

    private static void clearRegistry() throws Exception {
        Field field = ContextCacheMetricsRegistry.class.getDeclaredField("CACHE_MISS_INFO_METRICS");
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).clear();
    }

    private static class FakeMergedContextConfiguration extends MergedContextConfiguration {

        @Serial
        private static final long serialVersionUID = 1L;

        FakeMergedContextConfiguration() {
            super(null, null, null, null, null);
        }

        @Override
        public Class<?> getTestClass() {
            return FakeMergedContextConfiguration.class;
        }

        @Override
        public Class<?>[] getClasses() {
            return new Class<?>[0];
        }

        @Override
        public String[] getActiveProfiles() {
            return new String[0];
        }
    }
}
