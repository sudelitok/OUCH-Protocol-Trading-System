package bist.demo.exchange.matching.engine;


import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

public class Orderbook {

    private final String name;

    private final Hashtable<Integer, LinkedList<Integer>> buyOrders;   //fiyatlar ve aynı fiyata air miktarlar hastable'da tutulur,
    private final Hashtable<Integer, LinkedList<Integer>> sellOrders;
    private final LinkedList<Integer> buyPriceList;   //fiyatları ayrı bir dizide sıralı olarak tutabilmek için
    private final LinkedList<Integer> sellPriceList;
    private final Hashtable<Integer, LinkedList<Integer>> matchedList;

    public Orderbook(String name) {

        this.name = name;

        this.buyOrders = new Hashtable<>();
        this.sellOrders = new Hashtable<>();
        this.buyPriceList = new LinkedList<>();
        this.sellPriceList = new LinkedList<>();
        this.matchedList = new Hashtable<>();
    }

    public void handleNewOrder(byte side, int quantity, int price) {
        /*
         todo homework 2..
         */

        if (side == 'B') { // Buy Order
            addOrder(buyOrders, buyPriceList, price, quantity, true);
        }
        if (side == 'S') { // Sell Order
            addOrder(sellOrders, sellPriceList, price, quantity, false);
        }

        matchOrders();
    }

    public void print() {

        /*
         todo homework 2..
         */

        System.out.println("Orderbook name: " + name);

        System.out.println("\nBuy orders:");
        if (buyPriceList.isEmpty()) {
            System.out.println("No buy orders.");
        }
        else {
            int buyIndex = 1;
            for (int price : buyPriceList) {
                LinkedList<Integer> quantities = buyOrders.get(price);
                for (int quantity : quantities) {
                    System.out.println(buyIndex + ". Buy " + quantity + "@" + price);
                    buyIndex++;
                }
            }
        }

        System.out.println("\nSell orders:");
        if (sellPriceList.isEmpty()) {
            System.out.println("No sell orders.");
        }
        else {
            int sellIndex = 1;
            for (int price : sellPriceList) {
                LinkedList<Integer> quantities = sellOrders.get(price);
                for (int quantity : quantities) {
                    System.out.println(sellIndex + ". Sell " + quantity + "@" + price);
                    sellIndex++;
                }
            }
        }

        System.out.println("\nMatch orders:");
        if (matchedList.isEmpty()) {
            System.out.println("No match orders.");
        }
        else {
            int matchIndex = 1;
            for (int price : matchedList.keySet()) {
                LinkedList<Integer> quantities = matchedList.get(price);
                for (int quantity : quantities) {
                    System.out.println(matchIndex + ". Match " + quantity + "@" + price);
                    matchIndex++;
                }
            }
        }
    }

    private void addOrder(Hashtable<Integer, LinkedList<Integer>> orders, LinkedList<Integer> priceList, int price, int quantity, boolean isBuy) {
        if (!orders.containsKey(price)) {
            orders.put(price, new LinkedList<>());
            insertPrice(priceList, price, isBuy);
        }
        orders.get(price).add(quantity);  //fiyat daha önce buyOrder'a ve buyOrderList'e eklenmiş
    }

    private void insertPrice(LinkedList<Integer> priceList, int price, boolean isBuy) {
        int existingPrice;
        ListIterator<Integer> iterator = priceList.listIterator();
        while (iterator.hasNext()) {
            existingPrice = iterator.next();
            if ((isBuy && existingPrice < price) || (!isBuy && existingPrice > price)) {
                iterator.previous();
                iterator.add(price);
                return;
            }
        }
        priceList.add(price);
    }

    private void matchOrders() {
        Iterator<Integer> buyIterator = buyPriceList.iterator();
        while (buyIterator.hasNext()) {
            int buyPrice = buyIterator.next();
            Iterator<Integer> sellIterator = sellPriceList.iterator();

            while (sellIterator.hasNext()) {
                int sellPrice = sellIterator.next();

                if (buyPrice == sellPrice) {
                    LinkedList<Integer> buyQuantities = buyOrders.get(buyPrice);
                    LinkedList<Integer> sellQuantities = sellOrders.get(sellPrice);

                    int totalMatchedQuantity = 0;
                    while (!buyQuantities.isEmpty() && !sellQuantities.isEmpty()) {
                        int buyQuantity = buyQuantities.getFirst();
                        int sellQuantity = sellQuantities.getFirst();

                        int matchedQuantity = Math.min(buyQuantity, sellQuantity);
                        totalMatchedQuantity += matchedQuantity;

                        if (buyQuantity > matchedQuantity) {
                            buyQuantities.set(0, buyQuantity - matchedQuantity);
                        } else {
                            buyQuantities.removeFirst();
                        }

                        if (sellQuantity > matchedQuantity) {
                            sellQuantities.set(0, sellQuantity - matchedQuantity);
                        } else {
                            sellQuantities.removeFirst();
                        }
                    }

                    if (buyQuantities.isEmpty()) {
                        buyIterator.remove();
                        buyOrders.remove(buyPrice);
                    }

                    if (sellQuantities.isEmpty()) {
                        sellIterator.remove();
                        sellOrders.remove(sellPrice);
                    }

                    if (!matchedList.containsKey(buyPrice)) {
                        matchedList.put(buyPrice, new LinkedList<>());
                    }
                    matchedList.get(buyPrice).add(totalMatchedQuantity);

                    break;
                }

            }
        }
    }
}
