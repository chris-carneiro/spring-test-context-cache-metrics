package dev.silentcraft.tools.spring.test.context.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.test.context.MergedContextConfiguration;

@Component
public class ContextCacheMetricsRegistry {
    private static final Logger log = LoggerFactory.getLogger(ContextCacheMetricsRegistry.class);

    private static final Map<CacheMissInfoKey, CacheMissInfo> CACHE_MISS_INFO_METRICS = new ConcurrentHashMap<>();

    public void recordEntry(MergedContextConfiguration config) {
        CacheMissInfoKey key = new CacheMissInfoKey(config.getTestClass());
        CacheMissInfo.Entries entry = CacheMissInfo.Entries.fromConfig(config);

        CACHE_MISS_INFO_METRICS.compute(key, (testClass, info) -> {
            if (info == null) {
                return CacheMissInfo.withFirst(entry);
            }
            return info.withNew(entry);
        });

        log.debug("[OCC] Cache miss recorded for {}", key);
    }

    public static Map<CacheMissInfoKey, CacheMissInfo> snapshot() {
        return Map.copyOf(CACHE_MISS_INFO_METRICS);
    }
}
