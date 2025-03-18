package bist.demo.exchange.common.message;

public enum MessageParseResult {
    SUCCESSFUL,
    TOO_SMALL_MESSAGE,
    SIZE_MISMATCH,
    WRONG_MESSAGE_START,
    UNKNOWN_COMMAND,
    CRC_MISMATCH,
}
