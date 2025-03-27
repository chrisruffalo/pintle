package io.github.chrisruffalo.pintle.resolution.resolver;

import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.resolution.MdnsController;
import jakarta.enterprise.inject.spi.CDI;
import org.xbill.DNS.EDNSOption;
import org.xbill.DNS.Message;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.TSIG;


import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class MdnsResolver implements Resolver {

    public
    MdnsResolver(PintleConfig config, io.github.chrisruffalo.pintle.config.Resolver resolverConfig) {
        //super(config, resolverConfig);
    }

    /**
     * Sends a message to multiple servers, and queries are sent multiple times until either a
     * successful response is received, or it is clear that there is no successful response.
     *
     * @param query The query to send.
     * @param executor The service to use for async operations.
     * @return A future that completes when the query is finished.
     */
    public CompletionStage<Message> sendAsync(Message query, Executor executor) {
        // use cdi to lookup mdns controller
        final MdnsController controller = CDI.current().select(MdnsController.class).get();

        // query mdns
        Optional<Message> response = controller.query(query);

        final CompletableFuture<Message> result = new CompletableFuture<>();
        if (response.isPresent()) {
            result.complete(response.get());
        } else {
            result.complete(null);
        }

        return result;
    }

    @Override
    public void setPort(int i) {

    }

    @Override
    public void setTCP(boolean b) {

    }

    @Override
    public void setIgnoreTruncation(boolean b) {

    }

    @Override
    public void setEDNS(int i, int i1, int i2, List<EDNSOption> list) {

    }

    @Override
    public void setTSIGKey(TSIG tsig) {

    }

    @Override
    public void setTimeout(Duration duration) {

    }

    @Override
    public Message send(Message query) throws IOException {
        // use cdi to lookup mdns controller
        final MdnsController controller = CDI.current().select(MdnsController.class).get();

        // query mdns
        Optional<Message> response = controller.query(query);

        return response.orElse(null);
    }
}
