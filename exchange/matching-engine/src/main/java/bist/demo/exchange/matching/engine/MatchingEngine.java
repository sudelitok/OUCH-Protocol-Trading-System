package bist.demo.exchange.matching.engine;

import bist.demo.exchange.common.BufferHandler;
import bist.demo.exchange.common.ClientHandle;
import bist.demo.exchange.common.TcpServer;
import bist.demo.exchange.common.message.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class MatchingEngine implements BufferHandler {

    private final String host;

    private final int port;

    private final TcpServer tcpServer;

    private final OmnetMessageParser omnetMessageParser;
    private final OmnetMessageBuilder omnetMessageBuilder;

    private final Map<String, Orderbook> orderbookCache;

    public MatchingEngine(String host, int port) {
        this.host = host;
        this.port = port;

        tcpServer = new TcpServer("MatchingEngine", host, port, this);

        omnetMessageBuilder = new OmnetMessageBuilder();

        omnetMessageParser = new OmnetMessageParser(this::handleDefaultMessage);

        omnetMessageParser.addParser(OmnetCommand.Heartbeat, this::handleHeartbeat);
        omnetMessageParser.addParser(OmnetCommand.SendOrder, this::handleNewOrderRequest);

        orderbookCache = new HashMap<>();
    }

    @Override
    public void handleMessage(ClientHandle clientHandle, ByteBuffer buffer) {

        MessageParseResult messageParseResult = omnetMessageParser.parseReceivedMessage(clientHandle, buffer);

        if (messageParseResult != MessageParseResult.SUCCESSFUL) {
            System.out.printf("Message is malformed! Result: %s\n", messageParseResult);
        }
    }

    private void handleDefaultMessage(ClientHandle clientHandle, OmnetCommand command, ByteBuffer data) {
        System.out.printf("Un-registered %s command received. Size is %d\n", command, data.remaining());
    }

    private void handleHeartbeat(ClientHandle clientHandle, OmnetCommand command, ByteBuffer data) {

        int seqNum = data.getInt();

        System.out.printf("Heartbeat command received. seqNum: %d\n", seqNum);

        ByteBuffer replyData = ByteBuffer.allocate(4);

        replyData.putInt(seqNum + 1);

        ByteBuffer replyMessage = omnetMessageBuilder.prepareMessage(OmnetCommand.Heartbeat, replyData);

        clientHandle.send(replyMessage);
    }

    private void handleNewOrderRequest(ClientHandle clientHandle, OmnetCommand command, ByteBuffer data) {

        byte side = data.get();

        StringBuilder commodity = new StringBuilder();
        int quantity;
        int price;

        for (int i = 0; i < 5; i++) {
            commodity.append((char) data.get());
        }

        quantity = data.getInt();
        price = data.getInt();

        System.out.printf("New order command received. side: %c, commodity: %s, quantity: %d, price: %d\n",
                side, commodity, quantity, price);

        Orderbook orderbook = orderbookCache.computeIfAbsent(commodity.toString(), e -> new Orderbook(commodity.toString()));

        orderbook.handleNewOrder(side, quantity, price);

        orderbook.print();
    }

    public void start() throws IOException {

        tcpServer.start();
    }

    public void stop() throws IOException {

        tcpServer.stop();
    }
}
