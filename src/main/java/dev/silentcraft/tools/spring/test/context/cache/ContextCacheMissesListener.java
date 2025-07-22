package dev.silentcraft.tools.spring.test.context.cache;

import org.springframework.test.context.MergedContextConfiguration;

public interface ContextCacheMissesListener {
    void onCacheMiss(MergedContextConfiguration key);
}
