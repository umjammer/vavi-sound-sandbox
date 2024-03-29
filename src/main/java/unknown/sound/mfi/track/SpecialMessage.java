package unknown.sound.mfi.track;

import unknown.sound.Message;


public class SpecialMessage extends TrackMessage {
    public byte[] getData() {
        return messageDataBytes;
    }

    public void setData(byte[] data) {
        messageDataBytes = data;
    }

    @Override
    public byte[] toBytes() {
        byte[] bytes = {
                           (byte) getDeltaTime(), -1, -16,
                           (byte) (getData().length >> 8),
                           (byte) getData().length
                       };
        byte[] specialMessage = Message.arrayCopy(bytes, getData().length);
        System.arraycopy(getData(), 0, specialMessage, bytes.length,
                         getData().length);
        return specialMessage;
    }

    public SpecialMessage(int deltaTime, byte[] byteData) {
        super(deltaTime);
        setData(byteData);
    }
}
