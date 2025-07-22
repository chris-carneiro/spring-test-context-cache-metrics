package dev.silentcraft.tools.spring.test.context.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.test.context.MergedContextConfiguration;

@Component
public class DefaultContextCacheMissesListener implements ContextCacheMissesListener {
    private static final Logger log = LoggerFactory.getLogger(DefaultContextCacheMissesListener.class);

    private static final ContextCacheMetricsRegistry contextCacheMetricsRegistry = new ContextCacheMetricsRegistry();

    @Override
    public void onCacheMiss(MergedContextConfiguration config) {
        contextCacheMetricsRegistry.recordEntry(config);
    }
}
