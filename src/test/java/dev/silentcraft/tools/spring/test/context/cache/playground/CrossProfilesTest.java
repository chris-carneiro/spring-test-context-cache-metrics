package dev.silentcraft.tools.spring.test.context.cache.playground;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTest;

@ActiveProfiles( profiles = {"home", "beach"})
@CacheAwareSpringBootTest
class CrossProfilesTest {

    @Autowired
    private PalmTree palmTree;

    @Autowired
    private CoffeeTable coffeeTable;


    @Test
    void contextLoads() {
        Assertions.assertNotNull(palmTree, "palmTree is null");
        Assertions.assertNotNull(coffeeTable, "coffeeTable is null");
    }

    @Test
    void testBeansAreWellBehaved() {
        Assertions.assertEquals("Hello I'm a palm tree!", palmTree.helloYou());
        Assertions.assertEquals("Hello I'm a coffee table!", coffeeTable.helloYou());
    }

}
