package io.github.chrisruffalo.pintle.config.resolver;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.ResolverSource;
import io.github.chrisruffalo.pintle.config.diff.Diff;
import org.xbill.DNS.Resolver;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides the basic structure and methods for
 * the resolver source to include instantiating
 * child resolvers.
 *
 */
public abstract class BaseResolverSource implements ResolverSource {

    private String uri;

    private final AtomicReference<Resolver> resolverAtomicReference = new AtomicReference<>(null);

    @Override
    public String uri() {
        return uri;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @JsonIgnore
    protected abstract Resolver construct(PintleConfig config, io.github.chrisruffalo.pintle.config.Resolver resolverConfig);

    @Override
    public Resolver resolver(PintleConfig config, io.github.chrisruffalo.pintle.config.Resolver resolverConfig) {
        final Resolver r = resolverAtomicReference.compareAndExchange(null, construct(config, resolverConfig));
        if (r == null) {
            return resolverAtomicReference.get();
        }
        return r;
    }

    protected Diff internalDiff(ResolverSource other) {
        return new Diff("", Collections.emptySet());
    };

    protected Set<String> allProperties() {
        return new HashSet<>();
    };

    @Override
    public Diff diff(ResolverSource other) {
        final Set<String> diffSet = new HashSet<>();
        if(other == null) {
            diffSet.add("");
            diffSet.add("type");
            diffSet.addAll(allProperties());
        } else {
            if (!this.type().equals(other.type())) {
                diffSet.add("type");
                diffSet.addAll(allProperties());
            } else {
                diffSet.addAll(this.internalDiff(other).differences(false));
            }
        }
        return new Diff("matcher", diffSet);
    }
}
