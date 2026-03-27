package dev.silentcraft.tools.spring.test.context.cache;

/**
 * Represents the type of a context cache event observed during test suite execution.
 * <p>
 * Each value describes the outcome of a {@link org.springframework.test.context.cache.ContextCache}
 * lookup for a given {@link org.springframework.test.context.MergedContextConfiguration}.
 *
 * @see TestContextHistory
 * @see dev.silentcraft.tools.spring.test.context.cache.ContextCacheMetricsRegistry
 */
public enum EventType {

    /**
     * The first {@code ApplicationContext} built across all test classes in all modules
     * of the current project during a single build execution.
     * <p>
     * Recorded when a cache miss occurs and no context has ever been built in this JVM process.
     * Under normal circumstances there is exactly one {@code BUILD} event per build execution.
     */
    BUILD,

    /**
     * An {@code ApplicationContext} was built because an existing cached context could not be reused.
     * <p>
     * Spring determines cache identity from the full {@link org.springframework.test.context.MergedContextConfiguration}
     * of each test class. Any difference in the following causes a cache miss and therefore a {@code REBUILD}:
     * <ul>
     *   <li>Configuration classes ({@code @SpringBootTest(classes = ...)}, {@code @ContextConfiguration(classes = ...)})</li>
     *   <li>Active profiles ({@code @ActiveProfiles}, {@code spring.profiles.active})</li>
     *   <li>Property source locations ({@code @TestPropertySource(locations = ...)})</li>
     *   <li>Inline property overrides ({@code @TestPropertySource(properties = ...)})</li>
     *   <li>Context initializer classes ({@code @ContextConfiguration(initializers = ...)})</li>
     *   <li>Context customizers — includes {@code @MockBean}, {@code @SpyBean}, and {@code @Import} on the test class</li>
     *   <li>Context loader class</li>
     *   <li>Web environment ({@code @SpringBootTest(webEnvironment = ...)})</li>
     *   <li>Resource locations ({@code @ContextConfiguration(locations = ...)})</li>
     *   <li>Parent context configuration</li>
     *   <li>Explicit context eviction via {@code @DirtiesContext}</li>
     * </ul>
     */
    REBUILD,

    /**
     * An existing {@code ApplicationContext} was retrieved from the cache.
     * <p>
     * Recorded when a cache hit occurs — the requested configuration was already loaded
     * and Spring returned it without rebuilding.
     */
    REUSE
}
