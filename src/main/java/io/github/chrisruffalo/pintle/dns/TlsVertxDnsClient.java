package io.github.chrisruffalo.pintle.dns;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.PemTrustOptions;
import org.jboss.logging.Logger;

import java.util.Optional;

public class TlsVertxDnsClient extends VertxDnsClient {

    private final static Logger LOGGER = Logger.getLogger(TlsVertxDnsClient.class);

    private String trustPem;

    public void setTrustPem(final String trustPem) {
        this.trustPem = trustPem;
    }

    protected NetClientOptions createOptions() {
        final NetClientOptions options = super.createOptions();

        // enable tls
        options.setSsl(true);

        final JdkSSLEngineOptions engineOptions = new JdkSSLEngineOptions();
        options.setSslEngineOptions(engineOptions);

        Optional.ofNullable(trustPem).ifPresentOrElse(pemString -> {
            final PemTrustOptions trustOptions = new PemTrustOptions();
            trustOptions.addCertValue(Buffer.buffer(pemString));
            options.setTrustOptions(trustOptions);
        }, () -> {
            options.setTrustAll(true);
        });

        return options;
    }
}
