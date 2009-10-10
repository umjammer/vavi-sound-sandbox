package unknown.sound.mfi;

import unknown.sound.Message;


public class UnknownMessage extends Message {
    public byte[] toBytes() {
        return getDataBytes();
    }

    public UnknownMessage(byte[] dataBytes) {
        super(dataBytes);
    }
}

/* */
