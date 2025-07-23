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

public class CacheAwareSpringBootTestBootstrapper extends SpringBootTestContextBootstrapper {

    private static final Logger log = LoggerFactory.getLogger(CacheAwareSpringBootTestBootstrapper.class);

    private static final ObservableContextCache OBSERVABLE_CONTEXT_CACHE = new ObservableContextCache();

    private static final DefaultContextCacheMissesListener defaultContextCacheMissedListener = new DefaultContextCacheMissesListener();

    static {
        OBSERVABLE_CONTEXT_CACHE.registerListener(defaultContextCacheMissedListener);
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