package bist.demo.exchange.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TcpServer {

    private static final int BACKLOG_SIZE = 1000;

    private final String serverName;

    private final String host;

    private final int port;

    private final BufferHandler bufferHandler;

    private ScheduledExecutorService threadPool = null;

    private final AtomicInteger threadNumber = new AtomicInteger(1);

    private ServerSocket serverSocket = null;
    private boolean terminateAcceptor = false;

    private final List<ClientHandle> connectedClients = new ArrayList<>();
    private final Object clientLock = new Object();

    public TcpServer(String serverName, String host, int port, BufferHandler bufferHandler) {
        this.serverName = serverName;
        this.host = host;
        this.port = port;
        this.bufferHandler = bufferHandler;
    }

    public void stop() throws IOException {
        if (threadPool == null) {
            return;
        }

        terminateAcceptor = true;

        serverSocket.close();

        try {
            threadPool.shutdown();
        } catch (Exception exception) {
            System.err.println("Error while stopping thread. " + exception);
        } finally {
            threadPool = null;
        }
    }

    public void start() throws IOException {
        if (threadPool != null) {
            System.err.println("Already open!");
            return;
        }

        threadPool = Executors.newScheduledThreadPool(3,
                r -> new Thread(r, serverName + "-" + threadNumber.getAndIncrement()));

        serverSocket = new ServerSocket(port, BACKLOG_SIZE, InetAddress.getByName(host));

        System.out.printf("%s Server is opened at %s:%d.\n", serverName, host, port);

        threadPool.scheduleWithFixedDelay(this::handleConnections, 1000, 500, TimeUnit.MILLISECONDS);
        threadPool.scheduleWithFixedDelay(this::printStats, 5_000, 5_000, TimeUnit.MILLISECONDS);

        threadPool.schedule(this::acceptConnections, 1000, TimeUnit.MILLISECONDS);
    }

    private void printStats() {
        System.out.printf("Connected client count: %d\n", connectedClients.size());
    }

    private void acceptConnections() {

        System.out.println("Waiting clients.");

        while (true) {
            Socket clientSocket;
            try {
                clientSocket = serverSocket.accept();
                System.out.println("New client connected. " + clientSocket.toString());
                addClient(clientSocket);
            } catch (IOException e) {
                System.out.println("Client connection problem. " + e.getMessage());
            }

            if (terminateAcceptor) {
                System.out.println("Stopping acceptor.");
                break;
            }
        }
    }

    private void handleConnections() {

        synchronized (clientLock) {
            Iterator<ClientHandle> iterator = connectedClients.iterator();

            while (iterator.hasNext()) {
                ClientHandle next = iterator.next();

                boolean clientHandleStatus = next.handle();

                if (!clientHandleStatus) {
                    System.out.println("Client disconnected. " + next);
                    iterator.remove();
                    next.closeClient();
                }
            }
        }
    }

    private void addClient(Socket clientSocket) {
        synchronized (clientLock) {
            connectedClients.add(new ClientHandle(clientSocket, bufferHandler));
        }
    }
}
