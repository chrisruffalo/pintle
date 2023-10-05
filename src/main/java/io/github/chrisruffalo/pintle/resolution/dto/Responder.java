package io.github.chrisruffalo.pintle.resolution.dto;

import io.vertx.core.Future;
import org.xbill.DNS.Message;

public interface Responder {

    Future<Void> respond(final byte[] withBytes);

    Future<Void> respond(final Message withMessage);

}
