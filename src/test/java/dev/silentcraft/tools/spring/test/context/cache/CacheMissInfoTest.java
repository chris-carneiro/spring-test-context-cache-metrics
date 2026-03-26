package dev.silentcraft.tools.spring.test.context.cache;

import java.io.Serial;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.MergedContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CacheMissInfoTest {

    private static final CacheMissInfo.Entries ENTRY_A = new CacheMissInfo.Entries(
            Instant.now(), List.of("com.example.AppConfig"), List.of("test"));

    private static final CacheMissInfo.Entries ENTRY_B = new CacheMissInfo.Entries(
            Instant.now(), List.of("com.example.OtherConfig"), List.of("integration"));

    @Test
    void withFirst_shouldCreateInfoWithSingleEntry() {
        CacheMissInfo info = CacheMissInfo.withFirst(ENTRY_A);

        assertEquals(1, info.entries().size());
        assertEquals(ENTRY_A, info.entries().get(0));
    }

    @Test
    void withNew_shouldReturnNewInstanceWithAdditionalEntry() {
        CacheMissInfo original = CacheMissInfo.withFirst(ENTRY_A);

        CacheMissInfo updated = original.withNew(ENTRY_B);

        assertEquals(2, updated.entries().size());
        assertEquals(List.of(ENTRY_A, ENTRY_B), updated.entries());
    }

    @Test
    void withNew_shouldNotMutateOriginalInstance() {
        CacheMissInfo original = CacheMissInfo.withFirst(ENTRY_A);

        original.withNew(ENTRY_B);

        assertEquals(1, original.entries().size());
        assertEquals(List.of(ENTRY_A), original.entries());
    }

    @Test
    void entries_shouldReturnUnmodifiableList() {
        CacheMissInfo info = CacheMissInfo.withFirst(ENTRY_A);

        assertThrows(UnsupportedOperationException.class, () -> info.entries().add(ENTRY_B));
    }

    @Test
    void entries_classes_shouldReturnUnmodifiableList() {
        CacheMissInfo.Entries entry = new CacheMissInfo.Entries(Instant.now(), List.of("com.example.Config"), List.of("test"));

        assertThrows(UnsupportedOperationException.class, () -> entry.classes().add("com.example.Other"));
    }

    @Test
    void entries_activeProfiles_shouldReturnUnmodifiableList() {
        CacheMissInfo.Entries entry = new CacheMissInfo.Entries(Instant.now(), List.of("com.example.Config"), List.of("test"));

        assertThrows(UnsupportedOperationException.class, () -> entry.activeProfiles().add("extra"));
    }

    @Test
    void entries_fromConfig_shouldMapClassesAndProfiles() {
        MergedContextConfiguration config = new FakeMergedContextConfiguration(
                new Class<?>[]{ String.class },
                new String[]{ "zoo" }
        );

        CacheMissInfo.Entries entry = CacheMissInfo.Entries.fromConfig(config);

        assertNotNull(entry.timestamp());
        assertEquals(List.of(String.class.toString()), entry.classes());
        assertEquals(List.of("zoo"), entry.activeProfiles());
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
