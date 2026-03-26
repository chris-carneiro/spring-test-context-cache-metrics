package dev.silentcraft.tools.spring.test.context.cache.playground;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InheritedAnnotationBehaviorTest extends AbstractZooBehaviorTest {

    @Autowired
    private Bluebird bluebird;

    @Test
    void annotationInheritedFromParent() {
        Assertions.assertEquals("Hello I'm a bluebird!", bluebird.helloYou());
    }
}
