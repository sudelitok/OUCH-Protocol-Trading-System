package bist.demo.exchange.common;

import bist.demo.exchange.common.message.MessageParseResult;
import bist.demo.exchange.common.message.OmnetCommand;
import bist.demo.exchange.common.message.OmnetMessageParser;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class TcpClient {

    private final String host;

    private final int port;

    private Socket socket = null;

    private ClientHandle clientHandle;

    private final BufferHandler bufferHandler;

    public TcpClient(BufferHandler bufferHandler, String host, int port) {
        this.bufferHandler = bufferHandler;
        this.host = host;
        this.port = port;
    }

    public boolean connect() throws IOException {

        try {
            socket = new Socket(InetAddress.getByName(host), port);

            clientHandle = new ClientHandle(socket, bufferHandler);

            return true;
        } catch (ConnectException exception) {
            System.err.println("Connection failed: No server listening on the specified port." + exception.getMessage());

            socket = null;
            return false;
        }
    }

    public boolean send(ByteBuffer buffer) throws IOException {
        if (socket == null) {

            System.err.println("Connection is not available.");
            return false;
        }

        OutputStream outputStream = socket.getOutputStream();

        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        outputStream.write(bytes);

        return true;
    }

    public void close() throws IOException {
        if (socket == null) {
            return;
        }

        socket.close();
    }

    public boolean handle() {
        return clientHandle.handle();
    }
}
