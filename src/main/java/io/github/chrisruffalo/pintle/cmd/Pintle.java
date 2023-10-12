package io.github.chrisruffalo.pintle.cmd;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.yaml.YamlConfigSource;
import jakarta.enterprise.inject.spi.CDI;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@QuarkusMain
public class Pintle {

    public static void main(String... args) throws IOException {
        // parse launch args and get the file
        final String configPath = "sample-config.yml";
        final Path filePath = Paths.get(configPath);

        // create a smallrye configuration outside of Quarkus context
        final SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .withMappingIgnore("pintle.**")
                .withMapping(PintleConfig.class)
                .withSources(new YamlConfigSource(filePath.toUri().toURL()))
                ;

        // todo: validate configuration

        // load the config from smallrye
        final PintleConfig config = builder.build().getConfigMapping(PintleConfig.class);

        // ensure the loaded/validated configuration file is passed in to quarkus for smallrye
        System.setProperty("quarkus.config.locations", filePath.toAbsolutePath().toString());

        // launch quarkus
        Quarkus.run(Launch.class, args);
    }

}
