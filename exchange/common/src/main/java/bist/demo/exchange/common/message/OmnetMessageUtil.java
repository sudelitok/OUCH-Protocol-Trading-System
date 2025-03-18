package bist.demo.exchange.common.message;

import java.nio.ByteBuffer;

public class OmnetMessageUtil {

    public static byte calculateCrc(ByteBuffer buffer, int size) {
        byte crc = 0;

        for (int i = 0; i < size; i++) {
            crc ^= buffer.get(i);
        }
        return crc;
    }
}
