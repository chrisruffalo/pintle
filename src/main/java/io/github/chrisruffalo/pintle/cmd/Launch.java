package io.github.chrisruffalo.pintle.cmd;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

public class Launch implements QuarkusApplication {

    @Inject
    PintleConfig config;

    @Inject
    Logger logger;

    @Override
    public int run(String... args) throws Exception {
        // handle any remaining logic for different beans
        // or initialization

        // wait for exit (https://quarkus.io/guides/command-mode-reference#the-command-mode-lifecycle)
        Quarkus.waitForExit();

        return 0;
    }
}
