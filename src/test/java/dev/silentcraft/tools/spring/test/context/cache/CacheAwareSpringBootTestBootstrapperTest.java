package dev.silentcraft.tools.spring.test.context.cache;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.BootstrapContext;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate;
import org.springframework.test.context.web.WebAppConfiguration;

class CacheAwareSpringBootTestBootstrapperTest {

    // -------------------------------------------------------------------------
    // getAnnotation() — failing until args/useMainMethod fix is applied
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("argsSource")
    void shouldExposeArgsFromAnnotation(Class<?> testClass, String[] expectedArgs) {
        ExposedBootstrapper bootstrapper = createBootstrapper(testClass);

        SpringBootTest annotation = bootstrapper.getAnnotation(testClass);

        assertNotNull(annotation, "getAnnotation() must not return null for @CacheAwareSpringBootTest classes");
        assertArrayEquals(expectedArgs, annotation.args());
    }

    static Stream<Arguments> argsSource() {
        return Stream.of(
                Arguments.of(WithArgs.class, new String[]{"--custom.arg=hello"}),
                Arguments.of(WithDefaults.class, new String[]{})
        );
    }

    @ParameterizedTest
    @MethodSource("useMainMethodSource")
    void shouldExposeUseMainMethodFromAnnotation(Class<?> testClass, SpringBootTest.UseMainMethod expected) {
        ExposedBootstrapper bootstrapper = createBootstrapper(testClass);

        SpringBootTest annotation = bootstrapper.getAnnotation(testClass);

        assertNotNull(annotation, "getAnnotation() must not return null for @CacheAwareSpringBootTest classes");
        assertEquals(expected, annotation.useMainMethod());
    }

    static Stream<Arguments> useMainMethodSource() {
        return Stream.of(
                Arguments.of(WithUseMainMethodAlways.class, SpringBootTest.UseMainMethod.ALWAYS),
                Arguments.of(WithUseMainMethodWhenAvailable.class, SpringBootTest.UseMainMethod.WHEN_AVAILABLE),
                Arguments.of(WithDefaults.class, SpringBootTest.UseMainMethod.NEVER)
        );
    }

    // -------------------------------------------------------------------------
    // getWebEnvironment()
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("webEnvironmentSource")
    void shouldReadWebEnvironmentFromAnnotation(Class<?> testClass, SpringBootTest.WebEnvironment expected) {
        ExposedBootstrapper bootstrapper = createBootstrapper(testClass);

        SpringBootTest.WebEnvironment result = bootstrapper.getWebEnvironment(testClass);

        assertEquals(expected, result);
    }

    static Stream<Arguments> webEnvironmentSource() {
        return Stream.of(
                Arguments.of(WithWebEnvNone.class, SpringBootTest.WebEnvironment.NONE),
                Arguments.of(WithWebEnvMock.class, SpringBootTest.WebEnvironment.MOCK),
                Arguments.of(WithWebEnvRandomPort.class, SpringBootTest.WebEnvironment.RANDOM_PORT),
                Arguments.of(WithWebEnvDefinedPort.class, SpringBootTest.WebEnvironment.DEFINED_PORT)
        );
    }

    // -------------------------------------------------------------------------
    // getProperties()
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("propertiesSource")
    void shouldReadPropertiesFromAnnotation(Class<?> testClass, String[] expected) {
        ExposedBootstrapper bootstrapper = createBootstrapper(testClass);

        String[] result = bootstrapper.getProperties(testClass);

        assertArrayEquals(expected, result);
    }

    static Stream<Arguments> propertiesSource() {
        return Stream.of(
                Arguments.of(WithProperties.class, new String[]{"key=value"}),
                Arguments.of(WithValueAlias.class, new String[]{"key=value"}),
                Arguments.of(WithDefaults.class, new String[]{})
        );
    }

    // -------------------------------------------------------------------------
    // getClasses()
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnExplicitClassesFromAnnotation() {
        ExposedBootstrapper bootstrapper = createBootstrapper(WithExplicitClasses.class);

        Class<?>[] classes = bootstrapper.getClasses(WithExplicitClasses.class);

        assertArrayEquals(new Class<?>[]{String.class}, classes);
    }

    /**
     * {@code getClasses()} must NOT add {@code @Import} targets to the configuration classes array.
     * Spring's {@code ImportsContextCustomizer} already handles {@code @Import} on test classes.
     * Duplicating it here registers those beans twice — outside of Spring Boot's auto-configuration
     * ordering — which breaks beans that depend on conditional auto-configuration (e.g. SpringDoc's
     * {@code AbstractRequestService}) because their conditions ({@code @ConditionalOnWebApplication},
     * etc.) may not yet be satisfied at direct-class-load time.
     */
    @Test
    void getClassesMustNotIncludeImportAnnotationTargets() {
        ExposedBootstrapper bootstrapper = createBootstrapper(WithExplicitClassesAndImport.class);

        List<Class<?>> classes = Arrays.asList(bootstrapper.getClasses(WithExplicitClassesAndImport.class));

        assertTrue(classes.contains(String.class), "explicit classes must still be returned");
        assertFalse(classes.contains(Integer.class),
                "@Import targets must not appear in getClasses() — ImportsContextCustomizer handles them");
    }

    /**
     * Same contract as above but for the no-explicit-classes case, which mirrors real-world usage
     * such as:
     * <pre>
     * {@code @CacheAwareSpringBootTest}
     * {@code @Import(FaultyStorageTestConfig.class)}
     * public class FileStorageRollbackIntegrationTest { ... }
     * </pre>
     * When {@code getClasses()} adds {@code FaultyStorageTestConfig} to the primary configuration
     * array, it is also processed again by {@code ImportsContextCustomizer}. The double-registration
     * as a primary class (rather than an import) changes context-loading semantics and can prevent
     * the main application's component scan from registering beans such as {@code DocumentService}.
     */
    @Test
    void getClassesMustNotIncludeImportAnnotationTargetsWhenNoExplicitClassesDeclared() {
        ExposedBootstrapper bootstrapper = createBootstrapper(WithImport.class);

        Class<?>[] classes = bootstrapper.getClasses(WithImport.class);
        List<Class<?>> classList = classes != null ? Arrays.asList(classes) : List.of();

        assertFalse(classList.contains(Integer.class),
                "@Import targets must not appear in getClasses() even when no explicit classes are declared");
    }


    // -------------------------------------------------------------------------
    // verifyConfiguration()
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("invalidWebAppConfigSource")
    void shouldThrowWhenWebAppConfigurationCombinedWithListeningPort(Class<?> testClass) {
        ExposedBootstrapper bootstrapper = createBootstrapper(testClass);

        assertThrows(IllegalStateException.class, () -> bootstrapper.verifyConfiguration(testClass));
    }

    static Stream<Arguments> invalidWebAppConfigSource() {
        return Stream.of(
                Arguments.of(WithWebAppConfigAndDefinedPort.class),
                Arguments.of(WithWebAppConfigAndRandomPort.class)
        );
    }

    @ParameterizedTest
    @MethodSource("validWebAppConfigSource")
    void shouldNotThrowForValidWebAppConfigurationCombination(Class<?> testClass) {
        ExposedBootstrapper bootstrapper = createBootstrapper(testClass);

        assertDoesNotThrow(() -> bootstrapper.verifyConfiguration(testClass));
    }

    static Stream<Arguments> validWebAppConfigSource() {
        return Stream.of(
                Arguments.of(WithWebAppConfigAndMockEnv.class),
                Arguments.of(WithWebEnvDefinedPort.class)
        );
    }

    // -------------------------------------------------------------------------
    // getCacheAwareContextLoaderDelegate()
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnCacheAwareContextLoaderDelegate() {
        ExposedBootstrapper bootstrapper = createBootstrapper(WithDefaults.class);

        CacheAwareContextLoaderDelegate delegate = bootstrapper.getCacheAwareContextLoaderDelegate();

        assertNotNull(delegate);
        assertInstanceOf(DefaultCacheAwareContextLoaderDelegate.class, delegate);
    }

    // -------------------------------------------------------------------------
    // Annotated stubs
    // -------------------------------------------------------------------------

    @CacheAwareSpringBootTest
    static class WithDefaults {
    }

    @CacheAwareSpringBootTest(args = {"--custom.arg=hello"})
    static class WithArgs {
    }

    @CacheAwareSpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
    static class WithUseMainMethodAlways {
    }

    @CacheAwareSpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.WHEN_AVAILABLE)
    static class WithUseMainMethodWhenAvailable {
    }

    @CacheAwareSpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    static class WithWebEnvNone {
    }

    @CacheAwareSpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
    static class WithWebEnvMock {
    }

    @CacheAwareSpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    static class WithWebEnvRandomPort {
    }

    @CacheAwareSpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
    static class WithWebEnvDefinedPort {
    }

    @CacheAwareSpringBootTest(properties = {"key=value"})
    static class WithProperties {
    }

    @CacheAwareSpringBootTest("key=value")
    static class WithValueAlias {
    }

    @CacheAwareSpringBootTest(classes = {String.class})
    static class WithExplicitClasses {
    }

    @CacheAwareSpringBootTest
    @Import(Integer.class)
    static class WithImport {
    }

    @CacheAwareSpringBootTest(classes = {String.class})
    @Import(Integer.class)
    static class WithExplicitClassesAndImport {
    }

    @CacheAwareSpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
    @WebAppConfiguration
    static class WithWebAppConfigAndDefinedPort {
    }

    @CacheAwareSpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @WebAppConfiguration
    static class WithWebAppConfigAndRandomPort {
    }

    @CacheAwareSpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
    @WebAppConfiguration
    static class WithWebAppConfigAndMockEnv {
    }

    // -------------------------------------------------------------------------
    // Test infrastructure
    // -------------------------------------------------------------------------

    private static ExposedBootstrapper createBootstrapper(Class<?> testClass) {
        ExposedBootstrapper bootstrapper = new ExposedBootstrapper();
        bootstrapper.setBootstrapContext(new FakeBootstrapContext(testClass));
        return bootstrapper;
    }

    static class ExposedBootstrapper extends CacheAwareSpringBootTestBootstrapper {

        @Override
        public SpringBootTest getAnnotation(Class<?> testClass) {
            return super.getAnnotation(testClass);
        }

        @Override
        public SpringBootTest.WebEnvironment getWebEnvironment(Class<?> testClass) {
            return super.getWebEnvironment(testClass);
        }

        @Override
        public String[] getProperties(Class<?> testClass) {
            return super.getProperties(testClass);
        }

        @Override
        public Class<?>[] getClasses(Class<?> testClass) {
            return super.getClasses(testClass);
        }

        @Override
        public void verifyConfiguration(Class<?> testClass) {
            super.verifyConfiguration(testClass);
        }
    }

    static class FakeBootstrapContext implements BootstrapContext {

        private final Class<?> testClass;

        FakeBootstrapContext(Class<?> testClass) {
            this.testClass = testClass;
        }

        @Override
        public Class<?> getTestClass() {
            return testClass;
        }

        @Override
        public CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate() {
            return new DefaultCacheAwareContextLoaderDelegate();
        }
    }
}
