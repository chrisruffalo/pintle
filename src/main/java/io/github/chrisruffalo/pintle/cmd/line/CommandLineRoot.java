package io.github.chrisruffalo.pintle.cmd.line;

import com.beust.jcommander.Parameter;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.nio.file.Path;

/**
 * Options accepted at the command line by the Pintle main class.
 */
@RegisterForReflection
public class CommandLineRoot {

    @Parameter(names = {"-c", "--config"}, description = "path to the pintle configuration file (required)", required = true)
    public Path configuration;

    @Parameter(names = {"-h", "--help"}, description = "print this help message", help = true)
    public boolean help;

}
