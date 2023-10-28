package io.github.chrisruffalo.pintle.resolution.resolver;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.util.NameUtil;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.jboss.logging.Logger;
import org.xbill.DNS.*;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Wraps a DNS Resolver and gives it some ability to handle context
 * resolution directly while skipping domains it cannot resolve,
 * if needed.
 */
public class PintleResolver implements Resolver {

    private static final Logger log = Logger.getLogger(PintleResolver.class);

    private static class Resolution {
        private final Message query;
        private final int[] attempts;
        private final int retriesPerResolver;
        private final long endTime;
        private List<PintleResolver.ResolverEntry> resolvers;
        private int currentResolver;

        Resolution(PintleResolver eres, Message query) {
            resolvers = new ArrayList<>(eres.resolvers);
            endTime = System.nanoTime() + eres.timeout.toNanos();
            if (eres.loadBalance) {
                int start = eres.lbStart.updateAndGet(i -> (i + 1) % resolvers.size());
                if (start > 0) {
                    List<PintleResolver.ResolverEntry> shuffle = new ArrayList<>(resolvers.size());
                    for (int i = 0; i < resolvers.size(); i++) {
                        int pos = (i + start) % resolvers.size();
                        shuffle.add(resolvers.get(pos));
                    }

                    resolvers = shuffle;
                }
            } else {
                resolvers =
                        resolvers.stream()
                                .sorted(Comparator.comparingInt(re -> re.failures.get()))
                                .collect(Collectors.toList());
            }

            attempts = new int[resolvers.size()];
            retriesPerResolver = eres.retries;
            this.query = query;
        }

        /* Asynchronously sends a message. */
        private CompletionStage<Message> send(Executor executorService) {
            PintleResolver.ResolverEntry r = resolvers.get(currentResolver);
            log.debugf(
                    "Sending {}/{}, id={} to resolver {} ({}), attempt {} of {}",
                    query.getQuestion().getName(),
                    Type.string(query.getQuestion().getType()),
                    query.getHeader().getID(),
                    currentResolver,
                    r.resolver,
                    attempts[currentResolver] + 1,
                    retriesPerResolver);
            attempts[currentResolver]++;
            return r.resolver.sendAsync(query, executorService);
        }

        /* Start an asynchronous resolution */
        private CompletionStage<Message> startAsync(Executor executorService) {
            return send(executorService)
                    .handle((result, ex) -> handle(result, ex, executorService))
                    .thenCompose(Function.identity());
        }

        private CompletionStage<Message> handle(Message result, Throwable ex, Executor executorService) {
            AtomicInteger failureCounter = resolvers.get(currentResolver).failures;
            if (ex != null) {
                log.debugf(
                        "Failed to resolve {}/{}, id={} with resolver {} ({}) on attempt {} of {}, reason={}",
                        query.getQuestion().getName(),
                        Type.string(query.getQuestion().getType()),
                        query.getHeader().getID(),
                        currentResolver,
                        resolvers.get(currentResolver).resolver,
                        attempts[currentResolver],
                        retriesPerResolver,
                        ex.getMessage());

                failureCounter.incrementAndGet();

                if (endTime - System.nanoTime() < 0) {
                    CompletableFuture<Message> f = new CompletableFuture<>();
                    f.completeExceptionally(
                            new IOException(
                                    "Timed out while trying to resolve "
                                            + query.getQuestion().getName()
                                            + "/"
                                            + Type.string(query.getQuestion().getType())
                                            + ", id="
                                            + query.getHeader().getID()));
                    return f;
                } else {
                    // go to next resolver, until retries on all resolvers are exhausted
                    currentResolver = (currentResolver + 1) % resolvers.size();
                    if (attempts[currentResolver] < retriesPerResolver) {
                        return send(executorService)
                                .handle((r, t) -> handle(r, t, executorService))
                                .thenCompose(Function.identity());
                    }

                    CompletableFuture<Message> f = new CompletableFuture<>();
                    f.completeExceptionally(ex);
                    return f;
                }
            } else {
                failureCounter.updateAndGet(i -> i > 0 ? (int) Math.log(i) : 0);
                return CompletableFuture.completedFuture(result);
            }
        }
    }

    private static class ResolverEntry {
        private final Resolver resolver;
        private final AtomicInteger failures;

        ResolverEntry(Resolver r) {
            this.resolver = r;
            this.failures = new AtomicInteger(0);
        }

        @Override
        public String toString() {
            return resolver.toString();
        }
    }

    /**
     * Default timeout until resolving is aborted.
     *
     * @since 3.2
     */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Default timeout until resolving with one of the used resolvers fails.
     *
     * @since 3.2
     */
    public static final Duration DEFAULT_RESOLVER_TIMEOUT = Duration.ofSeconds(5);

    private final List<PintleResolver.ResolverEntry> resolvers = new CopyOnWriteArrayList<>();
    private final AtomicInteger lbStart = new AtomicInteger();
    private boolean loadBalance;
    private int retries = 3;
    private Duration timeout = DEFAULT_TIMEOUT;

    private final PintleConfig pintleConfig;

    private final io.github.chrisruffalo.pintle.config.Resolver resolverConfig;

    /**
     * Creates a new Extended Resolver. The default {@link ResolverConfig} is used to determine the
     * servers for which {@link SimpleResolver}s are initialized. The timeout for each server is
     * initialized with {@link #DEFAULT_RESOLVER_TIMEOUT}.
     */
    public PintleResolver(final PintleConfig config, final io.github.chrisruffalo.pintle.config.Resolver resolverConfig) {
        this.pintleConfig = config;
        this.resolverConfig = resolverConfig;
        resolverConfig.sources().ifPresent(list -> {
            resolvers.clear();
            resolvers.addAll(list.stream().filter(Objects::nonNull).map(rs -> rs.resolver(config, resolverConfig)).filter(Objects::nonNull).map(ResolverEntry::new).toList());
        });
        // go through all resolvers by default
        this.retries = resolvers.size();
    }

    public io.github.chrisruffalo.pintle.config.Resolver config(){
        return this.resolverConfig;
    }

    @Override
    public void setPort(int port) {
        for (PintleResolver.ResolverEntry re : resolvers) {
            re.resolver.setPort(port);
        }
    }

    @Override
    public void setTCP(boolean flag) {
        for (PintleResolver.ResolverEntry re : resolvers) {
            re.resolver.setTCP(flag);
        }
    }

    @Override
    public void setIgnoreTruncation(boolean flag) {
        for (PintleResolver.ResolverEntry re : resolvers) {
            re.resolver.setIgnoreTruncation(flag);
        }
    }

    @Override
    public void setEDNS(int version, int payloadSize, int flags, List<EDNSOption> options) {
        for (PintleResolver.ResolverEntry re : resolvers) {
            re.resolver.setEDNS(version, payloadSize, flags, options);
        }
    }

    @Override
    public void setTSIGKey(TSIG key) {
        for (PintleResolver.ResolverEntry re : resolvers) {
            re.resolver.setTSIGKey(key);
        }
    }

    @Override
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * Sets the timeout for the {@link PintleResolver}.
     *
     * <p>Note that this <i>only</i> sets the timeout for the {@link PintleResolver}, not the
     * individual {@link Resolver}s. If the timeout expires, the {@link PintleResolver} simply stops
     * retrying, it does not abort running queries. The timeout value must be larger than that for the
     * individual resolver to have any effect.
     *
     * @param timeout The amount of time to wait before sending further queries.
     */
    @Override
    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    /**
     * Sends a message to multiple servers, and queries are sent multiple times until either a
     * successful response is received, or it is clear that there is no successful response.
     *
     * @param query The query to send.
     * @return A future that completes when the query is finished.
     */
    @Override
    public CompletionStage<Message> sendAsync(Message query) {
        return sendAsync(query, Infrastructure.getDefaultWorkerPool());
    }

    /**
     * Sends a message to multiple servers, and queries are sent multiple times until either a
     * successful response is received, or it is clear that there is no successful response.
     *
     * @param query The query to send.
     * @param executor The service to use for async operations.
     * @return A future that completes when the query is finished.
     */
    @Override
    public CompletionStage<Message> sendAsync(Message query, Executor executor) {
        PintleResolver.Resolution res = new PintleResolver.Resolution(this, query);
        return res.startAsync(executor);
    }

    /** Returns the nth resolver used by this PintleResolver */
    public Resolver getResolver(int n) {
        if (n < resolvers.size()) {
            return resolvers.get(n).resolver;
        }
        return null;
    }

    /** Returns all resolvers used by this PintleResolver */
    public Resolver[] getResolvers() {
        return resolvers.stream().map(re -> re.resolver).toArray(Resolver[]::new);
    }

    /** Adds a new resolver to be used by this PintleResolver */
    public void addResolver(Resolver r) {
        resolvers.add(new PintleResolver.ResolverEntry(r));
    }

    /** Deletes a resolver used by this PintleResolver */
    public void deleteResolver(Resolver r) {
        resolvers.removeIf(re -> re.resolver == r);
    }

    /**
     * Gets whether the servers receive queries load balanced.
     *
     * @since 3.2
     */
    public boolean getLoadBalance() {
        return loadBalance;
    }

    /**
     * Sets whether the servers should be load balanced.
     *
     * @param flag If true, servers will be tried in round-robin order. If false, servers will always
     *     be queried in the same order.
     */
    public void setLoadBalance(boolean flag) {
        loadBalance = flag;
    }

    /**
     * Gets the number of retries sent to each server per query.
     *
     * @since 3.2
     */
    public int getRetries() {
        return retries;
    }

    /** Sets the number of retries sent to each server per query */
    public void setRetries(int retries) {
        this.retries = retries;
    }

    @Override
    public String toString() {
        return "PintleResolver of " + resolvers;
    }

    public boolean canServiceDomain(final Name queryDomain) {
        final Set<String> allowed = this.resolverConfig.domains().orElse(Collections.emptySet());
        if (allowed.isEmpty()) {
            return true;
        }
        return NameUtil.intersects(queryDomain, allowed) || resolvers.stream().anyMatch(resolverEntry -> {
            if (resolverEntry.resolver instanceof final PintleResolver pintleResolver) {
                return pintleResolver.canServiceDomain(queryDomain);
            }
            return false;
        });
    }
}
