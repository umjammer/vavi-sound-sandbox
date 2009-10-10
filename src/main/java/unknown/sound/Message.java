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
        for (int index = 0; index < bytes.length; index++) {
            int lower = bytes[index] & 0xf;
            int upper = (bytes[index] >> 4) & 0xf;
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
            switch (num) {
            case 10: // 
                hex = "a";
                break;
            case 11: // 
                hex = "b";
                break;
            case 12: // 
                hex = "c";
                break;
            case 13: // 
                hex = "d";
                break;
            case 14: // 
                hex = "e";
                break;
            case 15: // 
                hex = "f";
                break;
            default:
                hex = "R";
                break;
            }
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

/* */
