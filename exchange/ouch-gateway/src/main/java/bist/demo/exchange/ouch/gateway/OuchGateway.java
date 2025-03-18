package bist.demo.exchange.ouch.gateway;

import bist.demo.exchange.common.BufferHandler;
import bist.demo.exchange.common.ClientHandle;
import bist.demo.exchange.common.TcpClient;
import bist.demo.exchange.common.message.MessageParseResult;
import bist.demo.exchange.common.message.OmnetCommand;
import bist.demo.exchange.common.message.OmnetMessageBuilder;
import bist.demo.exchange.common.message.OmnetMessageParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OuchGateway implements BufferHandler {

    private final String host;

    private final int port;

    private TcpClient tcpClient;
    private boolean connectionStatus;

    private ScheduledExecutorService threadPool;

    private final OmnetMessageBuilder omnetMessageBuilder;
    private int heartbeatSeqNum = 1;

    private final OmnetMessageParser omnetMessageParser;

    public OuchGateway(String host, int port) {
        this.host = host;
        this.port = port;

        omnetMessageBuilder = new OmnetMessageBuilder();

        omnetMessageParser = new OmnetMessageParser(this::handleDefaultMessage);

        omnetMessageParser.addParser(OmnetCommand.Heartbeat, this::handleHeartbeat);
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

        if (seqNum == heartbeatSeqNum + 1) {
            heartbeatSeqNum++;
        }

    }

    public boolean connect() throws IOException {
        tcpClient = new TcpClient(this, host, port);

        connectionStatus = tcpClient.connect();

        System.out.printf("Connection status to %s:%d : %s\n", "127.0.0.1", 10000, connectionStatus);

        return connectionStatus;
    }

    public void start() {

        if (!connectionStatus) {
            System.out.println("Not connected to server.");
            return;
        }

        threadPool = Executors.newScheduledThreadPool(3, r -> new Thread(r, "Ouch-Gateway-Thread"));

        threadPool.scheduleWithFixedDelay(this::sendHeartbeat, 1_000, 5_000, TimeUnit.MILLISECONDS);

        threadPool.scheduleWithFixedDelay(this::handleConnection, 1_000, 500, TimeUnit.MILLISECONDS);

    }

    private void handleConnection() {

        if (tcpClient == null) {
            return;
        }

        boolean clientHandleStatus = tcpClient.handle();

        if (!clientHandleStatus) {
            System.out.println("Server disconnected. ");
            try {
                tcpClient.close();
            } catch (IOException e) {
                System.err.println("Closing client error");
            } finally {
                tcpClient = null;
            }
        }
    }

    public void sendOrder(String commodity, byte side, int quantity, int price) {

        ByteBuffer data = ByteBuffer.allocate(1 + 5 + 4 + 4);

        data.put(side);

        StringBuilder fixedSizeCommodityName = getCommodityName(commodity);

        for (int i = 0; i < 5; i++) {
            data.put((byte) fixedSizeCommodityName.charAt(i));
        }

        data.putInt(quantity);
        data.putInt(price);

        ByteBuffer wholeMessage = omnetMessageBuilder.prepareMessage(OmnetCommand.SendOrder, data);

        try {
            tcpClient.send(wholeMessage);
        } catch (IOException e) {
            System.out.println("Send error " + e.getMessage());
            System.out.println("Closing application.");
            System.exit(1);
        }
    }

    private static StringBuilder getCommodityName(String commodity) {
        StringBuilder fixedSizeCommodityName;

        if (commodity.length() >= 5) {
            fixedSizeCommodityName = new StringBuilder(commodity.substring(0, 5));
        } else {
            fixedSizeCommodityName = new StringBuilder(commodity);
        }

        int size = fixedSizeCommodityName.length();
        fixedSizeCommodityName.append(" ".repeat(Math.max(0, 5 - size)));
        return fixedSizeCommodityName;
    }

    private void sendHeartbeat() {
        ByteBuffer data = ByteBuffer.allocate(4);

        data.putInt(heartbeatSeqNum);

        ByteBuffer wholeMessage = omnetMessageBuilder.prepareMessage(OmnetCommand.Heartbeat, data);

        try {
            tcpClient.send(wholeMessage);
        } catch (IOException e) {
            System.out.println("Send error " + e.getMessage());
            System.out.println("Closing application.");
            System.exit(1);
        }
    }

    public void stop() throws IOException {

        threadPool.shutdown();

        tcpClient.close();

        threadPool = null;
        tcpClient = null;
    }
}
