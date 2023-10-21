package io.github.chrisruffalo.pintle.config;

import io.github.chrisruffalo.pintle.config.diff.Diff;
import io.github.chrisruffalo.pintle.config.producer.ConfigProducer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

class ConfigProducerTest {

    public static PintleConfig load(final String configName) {
        try {
            return ConfigProducer.load(Paths.get("src", "test", "resources", "config", configName));
        } catch (IOException e) {
            Assertions.fail(e.getMessage());
        }
        return null;
    }

    @Test
    public void difference() {
        final PintleConfig left = load("minimal-config.yml");
        final PintleConfig right = load("sample-config.yml");
        final Diff node = Diff.compare(left, right);
        System.out.printf("has changes: %s%n", node.changed());
    }
}