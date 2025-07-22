package dev.silentcraft.tools.spring.test.context.cache;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.test.context.MergedContextConfiguration;

public record CacheMissInfo(List<Entries> entries) {

    public CacheMissInfo(List<Entries> entries) {
        this.entries = List.copyOf(entries);
    }

    public static CacheMissInfo withFirst(Entries entries) {
        return new CacheMissInfo(List.of(entries));
    }

    @Override
    public List<Entries> entries() {
        return List.copyOf(entries);
    }

    public void addDetail(Entries detail) {
        entries.add(detail);
    }

    public CacheMissInfo withNew(Entries entry) {
        List<Entries> allEntries = new ArrayList<>(entries);
        allEntries.add(entry);
        return new CacheMissInfo(allEntries);
    }

    public record Entries(Instant timestamp, List<String> classes, List<String> activeProfiles) {

        public Entries(Instant timestamp, List<String> classes, List<String> activeProfiles) {
            this.timestamp = timestamp;
            this.classes = List.copyOf(classes);
            this.activeProfiles = List.copyOf(activeProfiles);
        }

        public static Entries fromConfig(MergedContextConfiguration config) {
            return new Entries(
                    Instant.now(),
                    Arrays.stream(config.getClasses()).map(Class::toString).toList(),
                    Arrays.asList(config.getActiveProfiles())
            );
        }

        @Override
        public List<String> classes() {
            return List.copyOf(classes);
        }

        @Override
        public List<String> activeProfiles() {
            return List.copyOf(activeProfiles);
        }
    }

}
