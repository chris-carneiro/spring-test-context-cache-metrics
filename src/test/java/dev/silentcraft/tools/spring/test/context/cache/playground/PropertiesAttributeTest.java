package dev.silentcraft.tools.spring.test.context.cache.playground;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTest;

@CacheAwareSpringBootTest(properties = "test.greeting=hello-from-annotation")
class PropertiesAttributeTest {

    @Autowired
    private Environment environment;

    @Test
    void propertyLandsInEnvironment() {
        Assertions.assertEquals("hello-from-annotation", environment.getProperty("test.greeting"));
    }
}
