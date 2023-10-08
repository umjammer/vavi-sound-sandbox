package unknown.sound.mfi.info;

import unknown.sound.Message;


public class CopyMessage extends InformationMessage {
    public String getCopy() {
        return messageDate;
    }

    public void setCopy(String copy) {
        messageDate = copy;
    }

    @Override
    public byte[] toBytes() {
        byte[] copyBytes = getCopy().getBytes();
        byte[] data = {
            100,
            111,
            112,
            121,
            (byte) (copyBytes.length >> 8),
            (byte) copyBytes.length
        };
        byte[] copyData = Message.arrayCopy(data, copyBytes.length);
        System.arraycopy(copyBytes, 0, copyData, data.length, copyBytes.length);
        return copyData;
    }

    private String messageDate;
    public static final String TAG = "copy";

    public CopyMessage(byte[] byteData) {
        super(byteData, "copy");
        setCopy(new String(byteData));
    }

    public CopyMessage(String copy) {
        setCopy(copy);
    }
}
