package dev.silentcraft.tools.spring.test.context.cache;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.BootstrapContext;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate;

public class CacheAwareSpringBootTestBootstrapper extends SpringBootTestContextBootstrapper {

    private static final Logger log = LoggerFactory.getLogger(CacheAwareSpringBootTestBootstrapper.class);

    private static final ObservableContextCache OBSERVABLE_CONTEXT_CACHE = new ObservableContextCache();

    private static final DefaultContextCacheMissesListener defaultContextCacheMissedListener = new DefaultContextCacheMissesListener();

    static {
        OBSERVABLE_CONTEXT_CACHE.registerListener(defaultContextCacheMissedListener);
    }

    @Override
    protected String[] getProperties(Class<?> testClass) {
        CacheAwareSpringBootTest cacheAwareSpringBootTest = TestContextAnnotationUtils.findMergedAnnotation(testClass, CacheAwareSpringBootTest.class);
        String[] props = null;
        if (cacheAwareSpringBootTest != null) {
            props = cacheAwareSpringBootTest.properties();
        }
        return props;
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

}