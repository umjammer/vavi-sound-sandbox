package unknown.sound.midi;

import unknown.sound.Message;


public class UnknownMessage extends Message {
    @Override
    public byte[] toBytes() {
        return getDataBytes();
    }

    public UnknownMessage(byte[] dataBytes) {
        super(dataBytes);
    }
}
