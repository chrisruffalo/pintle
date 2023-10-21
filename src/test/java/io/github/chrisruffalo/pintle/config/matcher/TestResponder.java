package io.github.chrisruffalo.pintle.config.matcher;

import io.github.chrisruffalo.pintle.model.ServiceType;
import io.github.chrisruffalo.pintle.resolution.responder.Responder;
import io.vertx.core.Future;
import org.xbill.DNS.Message;

public class TestResponder implements Responder {

    final String clientIp;

    public TestResponder(final String clientIp) {
        super();
        this.clientIp = clientIp;
    }

    @Override
    public Future<Void> respond(byte[] withBytes) {
        return Future.succeededFuture();
    }

    @Override
    public Future<Void> respond(Message withMessage) {
        return Future.succeededFuture();
    }

    @Override
    public ServiceType type() {
        return ServiceType.UDP;
    }

    @Override
    public String toClient() {
        return clientIp;
    }

    @Override
    public int onPort() {
        return 53;
    }
}
