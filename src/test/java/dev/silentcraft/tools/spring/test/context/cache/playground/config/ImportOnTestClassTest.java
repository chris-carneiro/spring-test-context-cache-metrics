package dev.silentcraft.tools.spring.test.context.cache.playground.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTest;
import dev.silentcraft.tools.spring.test.context.cache.playground.Bluebird;

@CacheAwareSpringBootTest
@Import(BeanConfiguration.class)
class ImportOnTestClassTest {

    @Autowired
    private Bluebird bluebird;

    @Test
    void contextLoadsViaBeanImport() {
        Assertions.assertNotNull(bluebird);
        Assertions.assertEquals("Hello I'm a bluebird!", bluebird.helloYou());
    }
}
