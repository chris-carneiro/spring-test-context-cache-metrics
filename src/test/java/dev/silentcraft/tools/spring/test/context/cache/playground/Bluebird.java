package dev.silentcraft.tools.spring.test.context.cache.playground;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("zoo")
@Component
public class Bluebird {
    public String helloYou() {
        return  "Hello I'm a bluebird!";
    }
}
