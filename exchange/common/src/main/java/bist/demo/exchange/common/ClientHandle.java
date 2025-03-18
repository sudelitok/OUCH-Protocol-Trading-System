package bist.demo.exchange.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ClientHandle {

    private final Socket socket;

    private final BufferHandler bufferHandler;

    private final ByteBuffer buffer = ByteBuffer.allocate(Constants.BUFFER_CAPACITY);

    private long expireTime;

    public ClientHandle(Socket socket, BufferHandler bufferHandler) {
        this.socket = socket;
        this.bufferHandler = bufferHandler;

        setExpireTime();
    }

    private void setExpireTime() {
        expireTime = System.currentTimeMillis() + Constants.KEEP_ALIVE_INTERVAL_MS;
    }

    private boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }

    public boolean handle() {

        if (isExpired()) {

            System.err.println("Client socket is disconnected. " + socket);
            return false;
        }

        InputStream inputStream;
        try {
            inputStream = socket.getInputStream();
        } catch (IOException e) {
            System.err.println("Input stream read error. " + e.getMessage());
            return false;
        }

        boolean readStatus = readIncomingMessage(inputStream);

        if (!readStatus) {
            System.err.println("Read incoming message error.");
            return false;
        }

        int size = buffer.remaining();

        if (size <= 0) {
            return true;
        }

        setExpireTime();

        bufferHandler.handleMessage(this, buffer);

        return true;
    }

    private boolean readIncomingMessage(InputStream inputStream) {
        int readByte;

        buffer.clear();

        while (true) {

            try {
                if (inputStream.available() <= 0) {
                    break;
                }
            } catch (IOException e) {
                return false;
            }

            try {
                readByte = inputStream.read();
            } catch (IOException e) {
                return false;
            }

            if (readByte == -1) {
                break;
            }

            buffer.put((byte) readByte);

        }
        buffer.flip();

        return true;
    }

    public void closeClient() {
        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Socket close error. " + e.getMessage());
        }
    }

    public void send(ByteBuffer message) {
        try {
            OutputStream outputStream = socket.getOutputStream();

            byte[] bytes = new byte[message.remaining()];
            message.get(bytes);

            outputStream.write(bytes);
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
    }

    @Override
    public String toString() {
        return "ClientHandle{" +
                "socket=" + socket +
                '}';
    }
}
