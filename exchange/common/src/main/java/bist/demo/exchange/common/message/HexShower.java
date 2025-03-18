package bist.demo.exchange.common.message;

public class HexShower {

    public static String convertToHexString(byte[] array) {

        if (array == null || array.length == 0) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append('[');
        for (int i = 0; i < array.length; i++) {

            stringBuilder.append(String.format("%02X", array[i]));

            if (i < array.length - 1) {
                stringBuilder.append(' ');
            }
        }
        stringBuilder.append(']');

        return stringBuilder.toString();
    }
}
