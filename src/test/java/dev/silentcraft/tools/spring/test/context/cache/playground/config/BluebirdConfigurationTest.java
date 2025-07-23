package dev.silentcraft.tools.spring.test.context.cache.playground.config;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import dev.silentcraft.tools.spring.test.context.cache.playground.Bluebird;

@SpringBootTest(classes= BeanConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK,
properties = {})
class BluebirdConfigurationTest {

    @Autowired
    private Bluebird bluebird;

    @Test
    void contextLoads() {
        Assertions.assertNotNull(bluebird);
    }

}
