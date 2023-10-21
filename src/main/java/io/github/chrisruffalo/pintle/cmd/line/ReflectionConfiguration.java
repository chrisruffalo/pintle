package io.github.chrisruffalo.pintle.cmd.line;

import com.beust.jcommander.converters.BooleanConverter;
import com.beust.jcommander.converters.PathConverter;
import com.beust.jcommander.validators.NoValueValidator;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * This class allows us to ensure that we register all
 * necessary JCommander classes necessary for reflection
 * at build time for native binaries
 */
@RegisterForReflection(targets = {
    NoValueValidator.class,
    PathConverter.class,
    BooleanConverter.class
})
public class ReflectionConfiguration {
}
