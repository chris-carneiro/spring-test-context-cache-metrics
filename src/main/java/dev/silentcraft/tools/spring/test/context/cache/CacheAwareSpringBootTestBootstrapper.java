package dev.silentcraft.tools.spring.test.context.cache;


import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.test.context.BootstrapContext;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Custom {@link org.springframework.boot.test.context.SpringBootTestContextBootstrapper}
 * that instruments the Spring {@link org.springframework.test.context.cache.ContextCache}
 * to track cache hits and misses during test execution.
 * <p>
 * This bootstrapper is used internally by the {@link dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTest}
 * annotation and replaces the default Spring Boot test bootstrapper to enhance observability
 * of the context reuse mechanism.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Registers an {@link ObservableContextCache} that exposes cache metrics to registered listeners.</li>
 *   <li>Integrates a {@link DefaultContextCacheMissesListener} to log context cache misses during test execution.</li>
 *   <li>Overrides {@link #getCacheAwareContextLoaderDelegate()} to inject the custom observable cache.</li>
 *   <li>Supports configuration via {@code classes}, {@code properties}, and {@code webEnvironment} from {@link SpringBootTest}.</li>
 *   <li>Automatically includes configuration classes annotated with {@link org.springframework.context.annotation.Import} on the test class.</li>
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
 * {@link org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate}
 * wrapping the observable cache:
 *
 * <pre>{@code
 * @Override
 * public CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate() {
 *     return new DefaultCacheAwareContextLoaderDelegate(OBSERVABLE_CONTEXT_CACHE);
 * }
 * }</pre>
 *
 * @see dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTest
 * @see dev.silentcraft.tools.spring.test.context.cache.ObservableContextCache
 * @see dev.silentcraft.tools.spring.test.context.cache.DefaultContextCacheMissesListener
 * @see org.springframework.boot.test.context.SpringBootTestContextBootstrapper
 * @see org.springframework.test.context.BootstrapWith
 */
public class CacheAwareSpringBootTestBootstrapper extends SpringBootTestContextBootstrapper {

    private static final Logger log = LoggerFactory.getLogger(CacheAwareSpringBootTestBootstrapper.class);

    private static final ObservableContextCache OBSERVABLE_CONTEXT_CACHE = new ObservableContextCache();

    private static final DefaultContextCacheMissesListener DEFAULT_CONTEXT_CACHE_MISSES_LISTENER = new DefaultContextCacheMissesListener();

    static {
        OBSERVABLE_CONTEXT_CACHE.registerListener(DEFAULT_CONTEXT_CACHE_MISSES_LISTENER);
    }


    @Override
    public CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate() {
        return new DefaultCacheAwareContextLoaderDelegate(OBSERVABLE_CONTEXT_CACHE);
    }

    @Override
    public void setBootstrapContext(BootstrapContext bootstrapContext) {
        log.info("[OCC] Bootstrap context with ObservableCache");
        super.setBootstrapContext(bootstrapContext);
    }

    /**
     * Resolves the set of configuration classes that will be used to load the {@code ApplicationContext}
     * for the test.
     *
     * <p>This override extends the default behavior of {@link SpringBootTestContextBootstrapper#getClasses(Class)}
     * by supporting additional class discovery mechanisms:</p>
     *
     * <ul>
     *   <li>If {@link dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTest#classes()} is explicitly set,
     *   those classes are used as the primary configuration source.</li>
     *   <li>If the test class is annotated with {@link org.springframework.context.annotation.Import},
     *   all referenced classes are added to the configuration set.</li>
     *   <li>If no explicit configuration classes are provided, the superclass logic is applied
     *   (e.g., auto-detection of a {@code @SpringBootConfiguration} class).</li>
     * </ul>
     *
     * <p>This design allows annotation-based configuration to remain declarative and composable,
     * while also respecting Spring Boot's built-in conventions.</p>
     *
     * @param testClass the test class being bootstrapped
     * @return the merged array of configuration classes to be used for the test context
     * @see dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTest
     * @see org.springframework.context.annotation.Import
     */
    @Override
    protected Class<?>[] getClasses(Class<?> testClass) {
        Set<Class<?>> classSet = new LinkedHashSet<>();

        CacheAwareSpringBootTest annotation = getWrappingAnnotation(testClass);
        boolean hasExplicitClasses = annotation != null && annotation.classes().length > 0;

        if (hasExplicitClasses) {
            classSet.addAll(Arrays.asList(annotation.classes()));
        }

        Import importAnnotation = AnnotatedElementUtils.findMergedAnnotation(testClass, Import.class);
        if (importAnnotation != null) {
            classSet.addAll(Arrays.asList(importAnnotation.value()));
        }

        if (!hasExplicitClasses) {
            Class<?>[] defaultClasses = super.getClasses(testClass);
            if (defaultClasses != null) {
                classSet.addAll(Arrays.asList(defaultClasses));
            }
        }

        return classSet.toArray(new Class<?>[0]);
    }


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

    protected CacheAwareSpringBootTest getWrappingAnnotation(Class<?> testClass) {
        return TestContextAnnotationUtils.findMergedAnnotation(testClass, CacheAwareSpringBootTest.class);
    }

    private static boolean hasListeningPortDefined(SpringBootTest.WebEnvironment webEnvironment) {
        return webEnvironment == SpringBootTest.WebEnvironment.DEFINED_PORT || webEnvironment == SpringBootTest.WebEnvironment.RANDOM_PORT;
    }

}