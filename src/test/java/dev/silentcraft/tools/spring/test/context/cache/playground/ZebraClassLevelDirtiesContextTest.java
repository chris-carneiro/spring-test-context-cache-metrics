package dev.silentcraft.tools.spring.test.context.cache.playground;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTest;

@ActiveProfiles("zoo")
@CacheAwareSpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ZebraClassLevelDirtiesContextTest {

    @Autowired
    private Zebra zebra;

    @Test
    void zebraIsWellBehaved() {
        Assertions.assertEquals("Hello I'm a Zebra!", zebra.helloYou());
    }
}
