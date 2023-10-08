package unknown.sound.midi.track;

import unknown.sound.Message;


public class QuaterFrameMessage extends SystemMessage {
    @Override
    public byte[] toBytes() {
        byte[] byteData = Message.arrayCopy(super.toBytes(), 2);
        int index = byteData.length - 1;
        byteData[index] = -15;
        byteData[index + 1] = getDataBytes()[0];
        return byteData;
    }

    public static final int LENGTH = 1;

    public QuaterFrameMessage(int deltaTime, byte[] data) {
        super(deltaTime, data);
    }
}
