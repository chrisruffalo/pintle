package io.github.chrisruffalo.pintle.resolution.responder;

import io.github.chrisruffalo.pintle.model.ServiceType;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

public class TcpResponder extends BaseResponder {

    final NetSocket socket;

    public TcpResponder(NetSocket socket, String toServer, int onPort) {
        super(toServer, onPort);
        this.socket = socket;
    }

    @Override
    public Future<Void> respond(byte[] withBytes) {
        // the tcp packets need to have the length sent back with them, which is the first two
        // bytes of the message. this means that we have to append two bytes to the current length
        // of the message and then add the length, spread across two bytes, to the message
        // the tcp client in dnsjava (NioTcpClient) references https://tools.ietf.org/html/rfc7766#section-8
        final Buffer messageWithPrependedLength = Buffer.buffer(2 + withBytes.length);
        messageWithPrependedLength.appendByte((byte) (withBytes.length >>> 8));
        messageWithPrependedLength.appendByte((byte) (withBytes.length & 0xFF));
        messageWithPrependedLength.appendBytes(withBytes);

        return socket.end(messageWithPrependedLength);
    }

    @Override
    public ServiceType type() {
        return ServiceType.TCP;
    }
}
