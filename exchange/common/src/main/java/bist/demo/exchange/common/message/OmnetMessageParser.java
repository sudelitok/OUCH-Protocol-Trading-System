package bist.demo.exchange.common.message;

import bist.demo.exchange.common.ClientHandle;
import bist.demo.exchange.common.Constants;

import java.nio.ByteBuffer;
import java.util.EnumMap;

public class OmnetMessageParser {

    private final ByteBuffer receivedData;

    private final EnumMap<OmnetCommand, OmnetMessageHandler> parsers;

    public OmnetMessageParser(OmnetMessageHandler defaultHandler) {

        receivedData = ByteBuffer.allocate(Constants.BUFFER_CAPACITY);

        parsers = new EnumMap<>(OmnetCommand.class);

        for (OmnetCommand omnetCommand : OmnetCommand.values()) {
            parsers.put(omnetCommand, defaultHandler);
        }
    }

    public void addParser(OmnetCommand command, OmnetMessageHandler handler) {
        parsers.put(command, handler);
    }

    public MessageParseResult parseReceivedMessage(ClientHandle clientHandle, ByteBuffer receivedMessage) {
        int wholeSize = receivedMessage.remaining();

        byte[] receivedBytes = new byte[wholeSize];
        for (int i = 0; i < wholeSize; i++) {
            receivedBytes[i] = receivedMessage.get(i);
        }

        System.out.printf("Received buffer(%d): %s\n", wholeSize, HexShower.convertToHexString(receivedBytes));

        if (wholeSize < Constants.OMNET_MESSAGE_MIN_LENGTH) {
            System.out.printf("Too small message. Size: %d\n", wholeSize);
            return MessageParseResult.TOO_SMALL_MESSAGE;
        }

        byte messageStart = receivedMessage.get();

        if (messageStart != Constants.OMNET_MESSAGE_START) {
            System.out.printf("Wrong message start %d != %d\n", messageStart, Constants.OMNET_MESSAGE_START);
            return MessageParseResult.WRONG_MESSAGE_START;
        }

        short dataSize = receivedMessage.getShort();

        if (dataSize + Constants.OMNET_MESSAGE_LENGTH_EXCEPT_DATA != wholeSize) {
            System.out.printf("Wrong size received (%d) != actual(%d)\n", dataSize, wholeSize - Constants.OMNET_MESSAGE_LENGTH_EXCEPT_DATA);
            return MessageParseResult.SIZE_MISMATCH;
        }

        byte commandValue = receivedMessage.get();
        OmnetCommand command = OmnetCommand.valueOf(commandValue);

        if (command == null) {
            System.out.printf("Unknown command. %d\n", commandValue);
            return MessageParseResult.UNKNOWN_COMMAND;
        }

        receivedData.clear();
        for (int i = 0; i < dataSize - 1; i++) {
            receivedData.put(receivedMessage.get());
        }

        byte receivedCrc = receivedMessage.get();
        byte calculatedCrc = OmnetMessageUtil.calculateCrc(receivedMessage, wholeSize - 1);

        if (receivedCrc != calculatedCrc) {
            System.out.printf("CRC mismatch. Received: %d, calculated: %d\n", receivedCrc, calculatedCrc);
            return MessageParseResult.UNKNOWN_COMMAND;
        }

        receivedData.flip();

        OmnetMessageHandler handler = parsers.get(command);

        handler.handle(clientHandle, command, receivedData);

        return MessageParseResult.SUCCESSFUL;
    }
}
