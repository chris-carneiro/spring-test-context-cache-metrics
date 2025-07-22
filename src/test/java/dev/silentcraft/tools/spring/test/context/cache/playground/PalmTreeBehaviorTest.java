package dev.silentcraft.tools.spring.test.context.cache.playground;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTest;

@ActiveProfiles(profiles = "beach")
@CacheAwareSpringBootTest
class PalmTreeBehaviorTest {

    @Autowired
    private PalmTree palmTree;

    @Test
    void testPalmTreeIsWellBehaved() {
        Assertions.assertEquals("Hello I'm a palm tree!", palmTree.helloYou());
    }

}
