package io.github.chrisruffalo.pintle.cmd;

import io.github.chrisruffalo.pintle.cmd.line.Args;
import io.github.chrisruffalo.pintle.cmd.line.CommandLineRoot;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * This serves as the Quarkus entrypoint that allows
 * some cross-over between plain Java and the ArC bean
 * container. This class takes the external options
 * and produces (or makes available) the configuration
 * that was loaded when the main class was launched.
 *
 */
@ApplicationScoped
public class Launch implements QuarkusApplication {

    static String[] args;

    static CommandLineRoot root;

    @Override
    public int run(String... args) throws Exception {
        // handle any remaining logic for different beans
        // or initialization

        // wait for exit (https://quarkus.io/guides/command-mode-reference#the-command-mode-lifecycle)
        Quarkus.waitForExit();

        return 0;
    }

    @Args
    @Produces
    public String[] produceArgs() {
        return args;
    }

    @Args
    @Produces
    public CommandLineRoot produceRoot() {
        return root;
    }

}
