package io.github.chrisruffalo.pintle.cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import io.github.chrisruffalo.pintle.cmd.line.CommandLineRoot;
import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.producer.ConfigProducer;
import io.github.chrisruffalo.pintle.util.PathUtil;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Since Quarkus doesn't _really_ have a way to
 * control pre-startup we use a main class to
 * bootstrap the application. This class is
 * responsible for parsing and validating the
 * configuration and deciding if the
 * application should start or not. It
 * also handles some command line options and
 * turns over the parsed command line options and
 * the loaded configuration to the Quarkus
 * application.
 */
@QuarkusMain
public class Pintle {

    public static void main(String... args) throws IOException {
        // parse command line options
        final CommandLineRoot root = new CommandLineRoot();
        final JCommander commander = JCommander.newBuilder()
                .programName("pintle")
                .addObject(root)
                .build();

        try {
            commander.parseWithoutValidation(args);
        } catch (ParameterException pe) {
            System.err.printf("incorrect command line arguments: %s%n", pe.getMessage());
            commander.usage();
            System.exit(1);
        }

        if (root.help) {
            commander.usage();
            System.exit(0);
        }

        if (root.configuration == null) {
            System.err.printf("a path to a configuration file is required%n");
            commander.usage();
            System.exit(1);
        }

        // parse launch args and get the file
        final Path filePath = PathUtil.real(root.configuration);
        if (filePath.equals(root.configuration)) {
            System.out.printf("using configuration file %s%n", filePath);
        } else {
            System.out.printf("using configuration file %s (%s)%n", root.configuration, filePath);
        }

        // load the config from smallrye
        final PintleConfig config = ConfigProducer.load(filePath);

        // todo: validate configuration


        Optional<Path> home = PathUtil.create("pintle home", config.etc().home(), null);
        if (home.isEmpty()) {
            System.exit(1);
        }
        Optional<Path> cache = PathUtil.create("pintle cache", home.get().resolve("cache"), null);
        if (cache.isEmpty()) {
            System.exit(1);
        }

        // ensure the loaded/validated configuration file is passed in to quarkus for smallrye
        System.setProperty("quarkus.config.locations", filePath.toAbsolutePath().toString());

        // this is super janky because it sets them in the static provider and uses them
        // on the other side from the static context. this should be viewed as the most
        // brittle possible way to do this.
        Launch.root = root;
        Launch.args = args;

        // launch quarkus
        Quarkus.run(Launch.class, args);
    }

}
