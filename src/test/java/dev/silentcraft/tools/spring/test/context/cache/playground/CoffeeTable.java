package dev.silentcraft.tools.spring.test.context.cache.playground;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("home")
public class CoffeeTable {

    public String helloYou() {
        return  "Hello I'm a coffee table!";
    }
}
