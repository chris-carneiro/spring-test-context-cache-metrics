package dev.silentcraft.tools.spring.test.context.cache.playground.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import dev.silentcraft.tools.spring.test.context.cache.playground.Bluebird;

@TestConfiguration
public class BeanConfiguration {

    @Bean
    public Bluebird bluebird() {
        return new Bluebird();
    }
}
