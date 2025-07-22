package dev.silentcraft.tools.spring.test.context.cache.playground.hit;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTest;
import dev.silentcraft.tools.spring.test.context.cache.playground.Zebra;

@ActiveProfiles("zoo")
@CacheAwareSpringBootTest
class ZebraBehaviorDirtiesContextTest {

    @Autowired
    private Zebra zebra;

    @Test
    @DirtiesContext
    void testDirtyZebraIsWellBehaved() {
        Assertions.assertEquals("Hello I'm a Zebra!", zebra.helloYou());
    }
}
