package dev.silentcraft.tools.spring.test.context.cache;

import java.io.Serial;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.MergedContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CacheAwareSpringBootTest
class TestContextHistoryTest {

    private static final TestContextHistory.Events EVENT_A = new TestContextHistory.Events(
            EventType.REUSE, Instant.now(), List.of("com.example.AppConfig"), List.of("test"));

    private static final TestContextHistory.Events MISS_EVENT_A = new TestContextHistory.Events(
            EventType.REBUILD, Instant.now(), List.of("com.example.AppConfig"), List.of("test"));

    private static final TestContextHistory.Events EVENT_B = new TestContextHistory.Events(
            EventType.REUSE, Instant.now(), List.of("com.example.OtherConfig"), List.of("integration"));

    @Test
    void withFirst_shouldCreateInfoWithSingleEvent() {
        TestContextHistory info = TestContextHistory.withFirst(EVENT_A);

        assertEquals(1, info.events().size());
        assertEquals(EVENT_A, info.events().get(0));
    }

    @Test
    void withNew_shouldReturnNewInstanceWithAdditionalEvent() {
        TestContextHistory original = TestContextHistory.withFirst(EVENT_A);

        TestContextHistory updated = original.withNew(EVENT_B);

        assertEquals(2, updated.events().size());
        assertEquals(List.of(EVENT_A, EVENT_B), updated.events());
    }

    @Test
    void withNew_shouldNotMutateOriginalInstance() {
        TestContextHistory original = TestContextHistory.withFirst(EVENT_A);
        original.withNew(EVENT_B);

        assertEquals(1, original.events().size());
        assertEquals(List.of(EVENT_A), original.events());
    }

    @Test
    void triggeredContextRebuild_returnsTrue_whenHistoryHasMissEvents() {
        TestContextHistory context = TestContextHistory.withFirst(EVENT_A);

        // WHEN
        boolean result = context.withNew(MISS_EVENT_A).triggeredContextRebuild();

        // THEN
        assertTrue(result);
    }

    @Test
    void triggeredContextRebuild_returnsFalse_whenHistoryHasOnlyHitEvents() {
        TestContextHistory context = TestContextHistory.withFirst(EVENT_A);

        // WHEN
        boolean result = context.withNew(EVENT_A).triggeredContextRebuild();

        // THEN
        Assertions.assertFalse(result);
    }

    @Test
    void events_shouldReturnUnmodifiableList() {
        TestContextHistory info = TestContextHistory.withFirst(EVENT_A);

        assertThrows(UnsupportedOperationException.class, () -> info.events().add(EVENT_B));
    }

    @Test
    void events_classes_shouldReturnUnmodifiableList() {
        TestContextHistory.Events event = new TestContextHistory.Events(EventType.REBUILD, Instant.now(), List.of("com.example.Config"), List.of("test"));

        assertThrows(UnsupportedOperationException.class, () -> event.classes().add("com.example.Other"));
    }

    @Test
    void events_activeProfiles_shouldReturnUnmodifiableList() {
        TestContextHistory.Events event = new TestContextHistory.Events(EventType.REBUILD, Instant.now(), List.of("com.example.Config"), List.of("test"));

        assertThrows(UnsupportedOperationException.class, () -> event.activeProfiles().add("extra"));
    }

    @Test
    void events_newMiss_shouldMapClassesAndProfiles() {
        MergedContextConfiguration config = new FakeMergedContextConfiguration(
                new Class<?>[]{String.class},
                new String[]{"zoo"}
        );

        TestContextHistory.Events event = TestContextHistory.Events.newMiss(config);

        assertNotNull(event.timestamp());
        assertEquals(List.of(String.class.toString()), event.classes());
        assertEquals(List.of("zoo"), event.activeProfiles());
    }

    @Test
    void events_newMiss_shouldMapEventTypeMiss() {
        MergedContextConfiguration config = new FakeMergedContextConfiguration(
                new Class<?>[]{String.class},
                new String[]{"zoo"}
        );

        TestContextHistory.Events event = TestContextHistory.Events.newMiss(config);

        assertNotNull(event.type());
        assertEquals(EventType.REBUILD, event.type());
    }

    @Test
    void events_newHit_shouldMapEventTypeHit() {
        MergedContextConfiguration config = new FakeMergedContextConfiguration(
                new Class<?>[]{String.class},
                new String[]{"zoo"}
        );

        TestContextHistory.Events event = TestContextHistory.Events.newHit(config);

        assertEquals(EventType.REUSE, event.type());
    }

    private static class FakeMergedContextConfiguration extends MergedContextConfiguration {

        @Serial
        private static final long serialVersionUID = -6645892394841526117L;
        private final Class<?>[] classes;
        private final String[] activeProfiles;

        FakeMergedContextConfiguration(Class<?>[] classes, String[] activeProfiles) {
            super(null, null, null, null, null);
            this.classes = classes;
            this.activeProfiles = activeProfiles;
        }

        @Override
        public Class<?>[] getClasses() {
            return classes;
        }

        @Override
        public String[] getActiveProfiles() {
            return activeProfiles;
        }
    }

}
