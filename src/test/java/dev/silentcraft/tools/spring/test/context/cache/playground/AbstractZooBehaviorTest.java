package dev.silentcraft.tools.spring.test.context.cache.playground;

import org.springframework.test.context.ActiveProfiles;

import dev.silentcraft.tools.spring.test.context.cache.CacheAwareSpringBootTest;

@ActiveProfiles("zoo")
@CacheAwareSpringBootTest
abstract class AbstractZooBehaviorTest {
}
