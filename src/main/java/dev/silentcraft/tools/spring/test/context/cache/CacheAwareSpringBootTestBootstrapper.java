package dev.silentcraft.tools.spring.test.context.cache;


import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.test.context.BootstrapContext;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.cache.ContextCache;
import org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Custom {@link SpringBootTestContextBootstrapper}
 * that instruments the Spring {@link ContextCache}
 * to track cache hits and misses during test execution.
 * <p>
 * This bootstrapper is used internally by the {@link CacheAwareSpringBootTest}
 * annotation and replaces the default Spring Boot test bootstrapper to enhance observability
 * of the context reuse mechanism.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Registers an {@link ObservableContextCache} that exposes cache metrics to registered listeners.</li>
 *   <li>Integrates a {@link DefaultContextCacheMissesListener} to log context cache misses during test execution.</li>
 *   <li>Overrides {@link #getCacheAwareContextLoaderDelegate()} to inject the custom observable cache.</li>
 *   <li>Supports configuration via {@code classes}, {@code properties}, and {@code webEnvironment} from {@link SpringBootTest}.</li>
 *   <li>Provides a safety check to avoid invalid use of {@code @WebAppConfiguration} with real servlet environments.</li>
 * </ul>
 *
 * <h2>Context Cache Instrumentation</h2>
 * The instrumentation of the context cache is achieved through a static initialization block:
 *
 * <pre>{@code
 * private static final DefaultContextCacheMissesListener DEFAULT_CONTEXT_CACHE_MISSES_LISTENER =
 *     new DefaultContextCacheMissesListener();
 *
 * static {
 *     OBSERVABLE_CONTEXT_CACHE.registerListener(DEFAULT_CONTEXT_CACHE_MISSES_LISTENER);
 * }
 * }</pre>
 * <p>
 * The registered {@link ObservableContextCache} is then injected into the Spring test context mechanism
 * via an override of {@link #getCacheAwareContextLoaderDelegate()}, which returns a
 * {@link DefaultCacheAwareContextLoaderDelegate}
 * wrapping the observable cache:
 *
 * <pre>{@code
 * @Override
 * public CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate() {
 *     return new DefaultCacheAwareContextLoaderDelegate(OBSERVABLE_CONTEXT_CACHE);
 * }
 * }</pre>
 *
 * @see CacheAwareSpringBootTest
 * @see ObservableContextCache
 * @see DefaultContextCacheMissesListener
 * @see SpringBootTestContextBootstrapper
 * @see BootstrapWith
 */
public class CacheAwareSpringBootTestBootstrapper extends SpringBootTestContextBootstrapper {

    private static final Logger log = LoggerFactory.getLogger(CacheAwareSpringBootTestBootstrapper.class);

    private static final ObservableContextCache OBSERVABLE_CONTEXT_CACHE = new ObservableContextCache();

    private static final DefaultContextCacheMissesListener DEFAULT_CONTEXT_CACHE_MISSES_LISTENER = new DefaultContextCacheMissesListener();

    private static volatile boolean activated;

    static {
        OBSERVABLE_CONTEXT_CACHE.registerListener(DEFAULT_CONTEXT_CACHE_MISSES_LISTENER);
        activated = false;
    }

    /**
     * Creates a new {@code CacheAwareSpringBootTestBootstrapper}.
     * Instantiated by the Spring test framework via reflection through {@link org.springframework.test.context.BootstrapWith}.
     */
    public CacheAwareSpringBootTestBootstrapper() {
    }

    /**
     * Returns {@code true} if this bootstrapper has been activated — i.e., at least one test class
     * annotated with {@link CacheAwareSpringBootTest} was bootstrapped in this JVM process.
     * <p>
     * Used by {@link dev.silentcraft.tools.junit.execution.listener.GlobalTestExecutionAnalyzer}
     * to guard against reporting when no {@code @CacheAwareSpringBootTest} tests were executed.
     *
     * @return {@code true} if the bootstrapper was invoked at least once
     */
    public static boolean isActivated() {
        return activated;
    }


    @Override
    public CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate() {
        return new DefaultCacheAwareContextLoaderDelegate(OBSERVABLE_CONTEXT_CACHE);
    }

    /**
     * Sets the bootstrap context and marks this bootstrapper as activated.
     * <p>
     * Called by the Spring test framework once per test class during the bootstrap phase.
     * Setting {@code activated = true} here ensures the flag is set as early as possible —
     * before any context is loaded.
     *
     * @param bootstrapContext the bootstrap context provided by the framework
     */
    @Override
    public void setBootstrapContext(BootstrapContext bootstrapContext) {
        log.debug("[OCC] Bootstrap context with ObservableCache");
        super.setBootstrapContext(bootstrapContext);
        activated = true;
    }

    /**
     * Resolves the set of configuration classes that will be used to load the {@code ApplicationContext}
     * for the test.
     *
     * <p>This override extends the default behavior of {@link SpringBootTestContextBootstrapper#getClasses(Class)}
     * with the following resolution strategy:</p>
     *
     * <ul>
     *   <li>If {@link CacheAwareSpringBootTest#classes()} is explicitly set,
     *   those classes are used as the primary configuration source.</li>
     *   <li>If no explicit configuration classes are provided, the superclass logic is applied
     *   (e.g., auto-detection of a {@code @SpringBootConfiguration} class).</li>
     * </ul>
     *
     * <p>{@code @Import} annotations on the test class are intentionally not processed here.
     * Spring Boot's {@code ImportsContextCustomizer} already handles them as part of the normal
     * context loading pipeline. Adding {@code @Import} targets to the primary classes array would
     * cause double-registration with different loading semantics, breaking beans that depend on
     * conditional auto-configuration.</p>
     *
     * @param testClass the test class being bootstrapped
     * @return the array of configuration classes to be used for the test context
     * @see CacheAwareSpringBootTest
     */
    @Override
    protected Class<?>[] getClasses(Class<?> testClass) {
        Set<Class<?>> classSet = new LinkedHashSet<>();

        CacheAwareSpringBootTest annotation = getWrappingAnnotation(testClass);
        boolean hasExplicitClasses = annotation != null && annotation.classes().length > 0;

        if (hasExplicitClasses) {
            classSet.addAll(Arrays.asList(annotation.classes()));
        }

        if (!hasExplicitClasses) {
            Class<?>[] defaultClasses = super.getClasses(testClass);
            if (defaultClasses != null) {
                classSet.addAll(Arrays.asList(defaultClasses));
            }
        }

        return classSet.toArray(new Class<?>[0]);
    }


    /**
     * Resolves the inline property overrides for the given test class.
     * <p>
     * If {@link CacheAwareSpringBootTest#properties()} is present on the test class,
     * those values are returned. Otherwise delegates to the superclass resolution.
     *
     * @param testClass the test class being bootstrapped
     * @return the array of {@code key=value} property overrides, or {@code null} if none
     */
    @Override
    protected String[] getProperties(Class<?> testClass) {
        CacheAwareSpringBootTest wrappingAnnotation = getWrappingAnnotation(testClass);
        String[] props = null;
        if (wrappingAnnotation != null) {
            props = wrappingAnnotation.properties();
        } else {
            props = super.getProperties(testClass);
        }
        return props;
    }

    /**
     * Resolves the {@link SpringBootTest.WebEnvironment} for the given test class.
     * <p>
     * If {@link CacheAwareSpringBootTest#webEnvironment()} is present, that value is used.
     * Otherwise delegates to the superclass resolution. Defaults to
     * {@link SpringBootTest.WebEnvironment#MOCK} when not explicitly set, matching the
     * behaviour of {@link SpringBootTest}.
     *
     * @param testClass the test class being bootstrapped
     * @return the resolved web environment
     */
    @Override
    protected SpringBootTest.WebEnvironment getWebEnvironment(Class<?> testClass) {
        CacheAwareSpringBootTest wrappingAnnotation = getWrappingAnnotation(testClass);
        SpringBootTest.WebEnvironment webEnvironment;

        if (wrappingAnnotation != null) {
            webEnvironment = wrappingAnnotation.webEnvironment();
        } else {
            webEnvironment = super.getWebEnvironment(testClass);
        }
        return webEnvironment;
    }

    /**
     * Validates the test class configuration before bootstrapping.
     * <p>
     * Rejects the combination of {@code @WebAppConfiguration} with a real servlet environment
     * ({@link SpringBootTest.WebEnvironment#DEFINED_PORT} or {@link SpringBootTest.WebEnvironment#RANDOM_PORT}),
     * as the two are mutually exclusive. Delegates all other validation to the superclass.
     *
     * @param testClass the test class being validated
     * @throws IllegalStateException if an incompatible combination is detected
     */
    @Override
    protected void verifyConfiguration(Class<?> testClass) {
        CacheAwareSpringBootTest wrappingAnnotation = getWrappingAnnotation(testClass);
        if (wrappingAnnotation != null && hasListeningPortDefined(wrappingAnnotation.webEnvironment())
                && MergedAnnotations.from(testClass, MergedAnnotations.SearchStrategy.INHERITED_ANNOTATIONS)
                .isPresent(WebAppConfiguration.class)) {
            throw new IllegalStateException("@WebAppConfiguration should only be used "
                    + "with @CacheAwareSpringBootTest when @CacheAwareSpringBootTest is configured with a "
                    + "mock web environment. Please remove @WebAppConfiguration or reconfigure @CacheAwareSpringBootTest.");
        }

        super.verifyConfiguration(testClass);
    }

    /**
     * Finds the {@link CacheAwareSpringBootTest} annotation on the given test class,
     * searching through meta-annotations and inherited annotations.
     *
     * @param testClass the test class to inspect
     * @return the resolved annotation, or {@code null} if not present
     */
    protected CacheAwareSpringBootTest getWrappingAnnotation(Class<?> testClass) {
        return TestContextAnnotationUtils.findMergedAnnotation(testClass, CacheAwareSpringBootTest.class);
    }

    /**
     * Synthesizes a {@link SpringBootTest} annotation from the attributes of {@link CacheAwareSpringBootTest}.
     * <p>
     * Required because Spring Boot's bootstrapper internally looks for a {@code @SpringBootTest}
     * annotation. This method bridges the two by synthesizing an equivalent {@code @SpringBootTest}
     * from the attributes declared on {@code @CacheAwareSpringBootTest}.
     *
     * @param testClass the test class being bootstrapped
     * @return a synthesized {@link SpringBootTest} instance, or {@code null} if the annotation is not present
     */
    @Override
    protected SpringBootTest getAnnotation(Class<?> testClass) {
        CacheAwareSpringBootTest cacheAware = getWrappingAnnotation(testClass);
        if (cacheAware == null) {
            return null;
        }

        Map<String, Object> annotationAttributes = AnnotationUtils.getAnnotationAttributes(cacheAware);
        return AnnotationUtils.synthesizeAnnotation(annotationAttributes, SpringBootTest.class, testClass);
    }

    private static boolean hasListeningPortDefined(SpringBootTest.WebEnvironment webEnvironment) {
        return webEnvironment == SpringBootTest.WebEnvironment.DEFINED_PORT || webEnvironment == SpringBootTest.WebEnvironment.RANDOM_PORT;
    }

}