package io.github.chrisruffalo.pintle.resolution.dto;

import io.vertx.core.Future;
import org.xbill.DNS.Message;

public abstract class BaseResponder implements Responder {

    private final String toServer;

    private final int onPort;

    public BaseResponder(String toServer, int onPort) {
        this.toServer = toServer;
        this.onPort = onPort;
    }

    public String getToServer() {
        return toServer;
    }

    public int getOnPort() {
        return onPort;
    }

    @Override
    public Future<Void> respond(Message withMessage) {
        return this.respond(withMessage.toWire());
    }
}
