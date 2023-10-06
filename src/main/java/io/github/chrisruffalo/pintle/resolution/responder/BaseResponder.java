package io.github.chrisruffalo.pintle.resolution.responder;

import io.vertx.core.Future;
import org.xbill.DNS.Message;

public abstract class BaseResponder implements Responder {

    private final String toClient;

    private final int onPort;

    public BaseResponder(String toClient, int onPort) {
        this.toClient = toClient;
        this.onPort = onPort;
    }

    public String toClient() {
        return toClient;
    }

    public int onPort() {
        return onPort;
    }

    @Override
    public Future<Void> respond(Message withMessage) {
        return this.respond(withMessage.toWire());
    }
}
