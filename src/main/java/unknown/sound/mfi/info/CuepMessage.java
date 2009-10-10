package unknown.sound.mfi.info;

import unknown.sound.Message;


public class CuepMessage extends InformationMessage {
    public byte[] getCuep() {
        return messageCuep;
    }

    public void setCuep(byte[] cuep) {
        messageCuep = cuep;
    }

    public byte[] toBytes() {
        byte[] cuepBytes = getCuep();
        byte[] data = {
                          100, 111, 112, 121, (byte) (cuepBytes.length >> 8),
                          (byte) cuepBytes.length
                      };
        byte[] cuepData = Message.arrayCopy(data, cuepBytes.length);
        System.arraycopy(cuepBytes, 0, cuepData, data.length, cuepBytes.length);
        return cuepData;
    }

    private byte[] messageCuep;
    public static final String TAG = "cuep";
    public static final byte[] MYBYTE = { 0, 0, 0, 64 };

    public CuepMessage(byte[] byteData) {
        super(byteData, "cuep");
        setCuep(byteData);
    }

    public CuepMessage() {
        this(MYBYTE);
    }
}

/* */
