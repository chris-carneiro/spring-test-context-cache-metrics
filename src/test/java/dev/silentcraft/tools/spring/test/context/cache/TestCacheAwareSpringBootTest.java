package dev.silentcraft.tools.spring.test.context.cache;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

class TestCacheAwareSpringBootTest {

    @BeforeEach
    void resetCounter() {
        CacheAwareSpringBootTestBootstrapperTester.counter.set(0);
    }

    @Test
    void shouldInvokeObservableCacheBootstrapper() {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(ObservableCacheContextTest.class))
                .build();

        Launcher launcher = LauncherFactory.create();
        launcher.execute(request);

        Assertions.assertTrue(CacheAwareSpringBootTestBootstrapperTester.counter.get() > 0);
    }

    public static class CacheAwareSpringBootTestBootstrapperTester extends CacheAwareSpringBootTestBootstrapper {

        public static final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public TestContext buildTestContext() {
            counter.incrementAndGet();
            return super.buildTestContext();
        }
    }

    @CASpringBootTest
    static class ObservableCacheContextTest {

        @Autowired
        ApplicationContext applicationContext;

        @Test
        void shouldBootstrapApplicationWithObservableCacheContext() {
            Assertions.assertNotNull(applicationContext, "The application context should have been autowired.");
            Assertions.assertEquals("Test observable spring-test context-cache", applicationContext.getId());
        }
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Inherited
    @BootstrapWith(CacheAwareSpringBootTestBootstrapperTester.class)
    @ExtendWith(SpringExtension.class)
    @ActiveProfiles("test")
    public @interface CASpringBootTest {
    }

}
