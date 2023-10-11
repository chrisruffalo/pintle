package io.github.chrisruffalo.pintle.resolution.responder;

import io.github.chrisruffalo.pintle.model.ServiceType;
import io.vertx.core.Future;
import org.xbill.DNS.Message;

public interface Responder {

    Future<Void> respond(final byte[] withBytes);

    Future<Void> respond(final Message withMessage);

    ServiceType type();

    String toClient();

    int onPort();

}
