package dev.silentcraft.tools.spring.test.context.cache.playground.hit;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTest;
import dev.silentcraft.tools.spring.test.context.cache.playground.Bluebird;

@ActiveProfiles("zoo")
@CacheAwareSpringBootTest
class BluebirdBehaviorTest {

    @Autowired
    private Bluebird bluebird;

    @Test
    void testBluebirdIsWellBehaved() {
        Assertions.assertEquals("Hello I'm a bluebird!", bluebird.helloYou());
    }
}
