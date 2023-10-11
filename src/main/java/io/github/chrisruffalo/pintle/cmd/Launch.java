package io.github.chrisruffalo.pintle.cmd;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;

public class Launch implements QuarkusApplication {

    @Override
    public int run(String... args) throws Exception {
        // handle any remaining logic for different beans
        // or initialization


        // wait for exit (https://quarkus.io/guides/command-mode-reference#the-command-mode-lifecycle)
        Quarkus.waitForExit();

        return 0;
    }
}
