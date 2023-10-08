package unknown.sound.midi.track;

import unknown.sound.Message;


public class SongPointerMessage extends SystemMessage {
    @Override
    public byte[] toBytes() {
        byte[] byteData = Message.arrayCopy(super.toBytes(), 3);
        int index = byteData.length - 2;
        byteData[index] = -14;
        byteData[index + 1] = getDataBytes()[0];
        byteData[index + 2] = getDataBytes()[1];
        return byteData;
    }

    public static final int LENGTH = 2;

    public SongPointerMessage(int deltaTime, byte[] data) {
        super(deltaTime, data);
    }
}
