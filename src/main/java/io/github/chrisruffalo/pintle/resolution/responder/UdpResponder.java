package io.github.chrisruffalo.pintle.resolution.responder;

import io.github.chrisruffalo.pintle.model.ServiceType;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;

public class UdpResponder extends BaseResponder{

    final DatagramSocket datagramSocket;

    public UdpResponder(DatagramSocket datagramSocket, String toServer, int onPort) {
        super(toServer, onPort);
        this.datagramSocket = datagramSocket;
    }

    @Override
    public Future<Void> respond(byte[] withBytes) {
        return datagramSocket.send(Buffer.buffer(withBytes), this.onPort(), this.toClient());
    }

    @Override
    public ServiceType type() {
        return ServiceType.UDP;
    }
}
