package dev.silentcraft.tools.spring.test.context.cache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@BootstrapWith(CacheAwareSpringBootTestBootstrapper.class)
@ExtendWith(SpringExtension.class)
public @interface CacheAwareSpringBootTest {

    Class<?>[] classes() default {};

    String[] properties() default {};

    SpringBootTest.WebEnvironment webEnvironment() default SpringBootTest.WebEnvironment.NONE;
}
