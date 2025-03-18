package bist.demo.exchange.ouch.gateway;

import bist.demo.exchange.common.Constants;
import bist.demo.exchange.common.TcpClient;
import bist.demo.exchange.common.message.HexShower;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class OuchGatewayMain {

    public static void main(String[] args) throws IOException {
        OuchGateway ouchGateway = new OuchGateway("127.0.0.1", 10000);

        boolean connect = ouchGateway.connect();

        if (!connect) {
            return;
        }

        ouchGateway.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        do {

            String command = reader.readLine();

            System.out.println(command);

            if ("x".equals(command)) {
                ouchGateway.stop();
                break;
            }

            if (command.startsWith("o:")) {
                sendOrder(ouchGateway, command);
            }

        } while (true);

    }

    /*
     o:b,25@100
     o:s,20@105
     */
    private static void sendOrder(OuchGateway ouchGateway, String order) {
        String[] split = order.split(":");

        if (split.length != 2) {
            System.err.println("wrong command!");
            return;
        }

        String[] orderTokens = split[1].split(",");

        if (orderTokens.length != 2) {
            System.err.println("wrong order details!");
            return;
        }

        byte side = Constants.SIDE_SELL;
        int quantity;
        int price;

        /*
         todo verify 'S'
         */
        if ("B".equals(orderTokens[0]) || "b".equals(orderTokens[0])) {
            side = Constants.SIDE_BUY;
        }

        String[] prices = orderTokens[1].split("@");

        if (prices.length != 2) {
            System.err.println("wrong price details!");
            return;
        }

        try {
            quantity = Integer.parseInt(prices[0]);
            price = Integer.parseInt(prices[1]);
        } catch (Exception exception) {
            System.err.println("parsing error! " + exception.getMessage());
            return;
        }

        ouchGateway.sendOrder("APPLE", side, quantity, price);
    }
}
