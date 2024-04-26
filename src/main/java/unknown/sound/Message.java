package unknown.sound;

import java.lang.reflect.Array;


public abstract class Message {
    public byte[] getDataBytes() {
        return messageDataBytes;
    }

    protected void setDataBytes(byte[] bytes) {
        messageDataBytes = bytes;
    }

    public abstract byte[] toBytes();

    public String toHex() {
        byte[] bytes = toBytes();
        StringBuilder buffer = new StringBuilder();
        for (byte aByte : bytes) {
            int lower = aByte & 0xf;
            int upper = (aByte >> 4) & 0xf;
            String hex = getHex(upper) + getHex(lower);
            buffer.append(hex);
        }

        return new String(buffer);
    }

    private static String getHex(int num) {
        String hex;
        if (num < 10) {
            hex = String.valueOf(num);
        } else {
            hex = switch (num) {
                case 10 -> //
                        "a";
                case 11 -> //
                        "b";
                case 12 -> //
                        "c";
                case 13 -> //
                        "d";
                case 14 -> //
                        "e";
                case 15 -> //
                        "f";
                default -> "R";
            };
        }
        return hex;
    }

    public String toString() {
        return getClass().getName();
    }

    public static byte[] getByteArray(int dataLength) {
        return (byte[]) Array.newInstance(Byte.TYPE, dataLength);
    }

    public static byte[] arrayCopy(byte[] dataBytes, int dataLength) {
        byte[] newBytes = getByteArray(dataLength + dataBytes.length);
        System.arraycopy(dataBytes, 0, newBytes, 0, dataBytes.length);
        return newBytes;
    }

    protected byte[] messageDataBytes;

    public Message(byte[] dataBytes) {
        setDataBytes(dataBytes);
    }

    protected Message() {
        this(new byte[0]);
    }
}
