package io.github.chrisruffalo.pintle;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.producer.ConfigProducer;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.nio.file.Paths;

public class ConfigLoader {

    public static PintleConfig load(final String configName) {
        try {
            return ConfigProducer.load(Paths.get("src", "test", "resources", "config", configName));
        } catch (IOException e) {
            Assertions.fail(e.getMessage());
        }
        return null;
    }

}
