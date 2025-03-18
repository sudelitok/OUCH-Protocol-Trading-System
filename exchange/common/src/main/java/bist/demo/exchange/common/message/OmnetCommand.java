package bist.demo.exchange.common.message;

public enum OmnetCommand {
    Heartbeat((byte) 0x48),
    SendOrder((byte) 0x4F);

    private final byte value;

    OmnetCommand(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    public static OmnetCommand valueOf(byte value) {

        for (OmnetCommand omnetCommand : values()) {
            if (value == omnetCommand.value) {
                return omnetCommand;
            }
        }

        return null;
    }
}
