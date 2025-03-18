package bist.demo.exchange.common.message;

import bist.demo.exchange.common.ClientHandle;

import java.nio.ByteBuffer;

public interface OmnetMessageHandler {

    void handle(ClientHandle clientHandle, OmnetCommand command, ByteBuffer data);
}
