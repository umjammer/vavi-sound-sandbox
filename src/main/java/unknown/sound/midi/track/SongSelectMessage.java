package unknown.sound.midi.track;

import unknown.sound.Message;


public class SongSelectMessage extends SystemMessage {
    public byte[] toBytes() {
        byte[] byteData = Message.arrayCopy(super.toBytes(), 2);
        int index = byteData.length - 1;
        byteData[index] = -13;
        byteData[index + 1] = getDataBytes()[0];
        return byteData;
    }

    public static final int LENGTH = 1;

    public SongSelectMessage(int deltaTime, byte[] data) {
        super(deltaTime, data);
    }
}
