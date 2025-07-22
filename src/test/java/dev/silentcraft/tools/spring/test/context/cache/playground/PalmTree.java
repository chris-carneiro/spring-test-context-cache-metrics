package dev.silentcraft.tools.spring.test.context.cache.playground;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("beach")
@Component
public class PalmTree {

       public String helloYou() {
        return  "Hello I'm a palm tree!";
    }
}
