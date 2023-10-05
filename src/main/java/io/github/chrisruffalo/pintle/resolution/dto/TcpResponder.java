package io.github.chrisruffalo.pintle.resolution.dto;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import org.jboss.logging.Logger;
import org.xbill.DNS.Message;

public class TcpResponder extends BaseResponder {

    final NetSocket socket;

    public TcpResponder(NetSocket socket, String toServer, int onPort) {
        super(toServer, onPort);
        this.socket = socket;
    }

    @Override
    public Future<Void> respond(byte[] withBytes) {
        final Logger logger = Logger.getLogger(this.getClass());
        final Buffer messageWithPrependedLength = Buffer.buffer(2 + withBytes.length);
        messageWithPrependedLength.appendByte((byte) ((withBytes.length >> 8) & 0xFF));
        messageWithPrependedLength.appendByte((byte) (withBytes.length & 0xFF));
        messageWithPrependedLength.appendBytes(withBytes);

        return socket.end(messageWithPrependedLength);
    }
}
