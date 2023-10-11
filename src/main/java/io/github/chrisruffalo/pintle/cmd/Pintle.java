package io.github.chrisruffalo.pintle.cmd;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Pintle {

    public static void main(String... args) {
        // parse launch args

        // ensure that files and paths are where they need to be
        System.setProperty("quarkus.config.locations", "sample-config.yml");

        // launch quarkus
        Quarkus.run(Launch.class, args);
    }

}
