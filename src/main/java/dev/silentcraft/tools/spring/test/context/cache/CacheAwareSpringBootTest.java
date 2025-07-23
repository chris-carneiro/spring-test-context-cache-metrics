package dev.silentcraft.tools.spring.test.context.cache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Custom annotation that wraps {@link org.springframework.boot.test.context.SpringBootTest}
 * to enable enhanced context cache observability during test execution.
 * <p>
 * {@code @CacheAwareSpringBootTest} behaves like {@code @SpringBootTest}, but integrates
 * a custom {@link org.springframework.test.context.BootstrapWith} strategy that allows
 * tracking of {@link org.springframework.test.context.cache.ContextCache} misses.
 * This helps identify misconfigured or inefficient test classes that prevent Spring
 * from reusing contexts between tests.
 * <p>
 * Typical usage is for integration or system tests where full Spring Boot context
 * loading is required, with the added benefit of observing cache behavior.
 * <p>
 * Unless explicitly set, {@link #webEnvironment()} defaults to
 * {@link org.springframework.boot.test.context.SpringBootTest.WebEnvironment#NONE}.
 *
 * <pre>
 * &#64;CacheAwareSpringBootTest(classes = MyApplication.class, webEnvironment = WebEnvironment.MOCK)
 * public class MyIntegrationTest {
 *     // ...
 * }
 * </pre>
 *
 * @see org.springframework.boot.test.context.SpringBootTest
 * @see org.springframework.test.context.BootstrapWith
 * @see dev.silentcraft.tools.spring.test.context.cache.ObservableContextCache
 * @see dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTestBootstrapper
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@BootstrapWith(CacheAwareSpringBootTestBootstrapper.class)
@ExtendWith(SpringExtension.class)
public @interface CacheAwareSpringBootTest {

    /**
     * Alias for {@link #properties()}.
     *
     * @return inline properties to be added to the {@code Environment} before the test context is loaded
     */
    @AliasFor("properties")
    String[] value() default {};

    /**
     * The component classes to use for loading an {@code ApplicationContext}.
     *
     * @return the classes to load
     */
    Class<?>[] classes() default {};

    /**
     * Properties in key-value format to be added to the {@code Environment} before the context is loaded.
     *
     * @return the properties
     */
    @AliasFor("value")
    String[] properties() default {};

    /**
     * Command-line arguments to be passed to the application under test.
     *
     * @return the arguments
     */
    String[] args() default {};

    /**
     * Determines whether the application's {@code main} method should be invoked.
     *
     * @return the {@code UseMainMethod} strategy
     */
    SpringBootTest.UseMainMethod useMainMethod() default SpringBootTest.UseMainMethod.NEVER;

    /**
     * The type of web environment to create for the test.
     *
     * @return the {@code WebEnvironment} mode
     */
    SpringBootTest.WebEnvironment webEnvironment() default SpringBootTest.WebEnvironment.NONE;
}

