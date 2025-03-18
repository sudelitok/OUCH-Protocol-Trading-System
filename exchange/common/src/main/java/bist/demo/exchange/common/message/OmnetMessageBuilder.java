package bist.demo.exchange.common.message;

import bist.demo.exchange.common.Constants;

import java.nio.ByteBuffer;

public class OmnetMessageBuilder {


    private final ByteBuffer sendMessage;

    public OmnetMessageBuilder() {

        sendMessage = ByteBuffer.allocate(Constants.BUFFER_CAPACITY);
    }

    public ByteBuffer prepareMessage(OmnetCommand omnetCommand, ByteBuffer data) {

        data.flip();

        sendMessage.clear();

        sendMessage.put(Constants.OMNET_MESSAGE_START);

        short dataSize = (short) (1 + data.remaining());

        sendMessage.putShort(dataSize);
        sendMessage.put(omnetCommand.getValue());

        sendMessage.put(data);

        byte crc = OmnetMessageUtil.calculateCrc(sendMessage, 1 + 2 + dataSize);

        sendMessage.put(crc);
        sendMessage.flip();


        return sendMessage;
    }
}
