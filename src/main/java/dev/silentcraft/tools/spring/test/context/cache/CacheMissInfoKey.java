package dev.silentcraft.tools.spring.test.context.cache;

/**
 * A simple key used to group cache miss metrics by test class.
 * <p>
 * This record is used internally by {@link ContextCacheMetricsRegistry} to
 * aggregate all context cache misses triggered by a given test class.
 *
 * @param testClass the test class associated with the cache miss.
 */
public record CacheMissInfoKey(Class<?> testClass) {
}
