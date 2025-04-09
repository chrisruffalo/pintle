package io.github.chrisruffalo.pintle.config.sources;


import io.github.chrisruffalo.pintle.ConfigLoader;
import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.Resolver;
import io.github.chrisruffalo.pintle.config.ResolverSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class ResolverSourceTest {

    @Test
    void googleSource() {
        final PintleConfig config = ConfigLoader.load("minimal-config.yml");
        Assertions.assertTrue(config.resolvers().isPresent());
        List<Resolver> resolverConfigurations = config.resolvers().get();
        Assertions.assertEquals(1, resolverConfigurations.size());
        final Resolver resolver = resolverConfigurations.get(0);
        Assertions.assertEquals("google", resolver.name());
        Assertions.assertTrue(resolver.sources().isPresent());
        final List<ResolverSource> sources = resolver.sources().get();
        Assertions.assertEquals(2, sources.size());
        ResolverSource source = sources.get(0);
        Assertions.assertNotNull(source);
        org.xbill.DNS.Resolver constructed = source.resolver(config, resolver);
        Assertions.assertNotNull(constructed);
    }

}
