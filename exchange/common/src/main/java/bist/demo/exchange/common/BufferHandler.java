package bist.demo.exchange.common;

import java.nio.ByteBuffer;

public interface BufferHandler {
    void handleMessage(ClientHandle clientHandle, ByteBuffer buffer);
}
