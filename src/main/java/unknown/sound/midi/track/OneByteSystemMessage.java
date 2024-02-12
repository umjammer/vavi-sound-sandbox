package unknown.sound.midi.track;

import unknown.sound.Message;


public class OneByteSystemMessage extends SystemMessage {
    @Override
    public byte[] toBytes() {
        byte[] byteData = Message.arrayCopy(super.toBytes(), 1);
        int index = byteData.length;
        byteData[index] = (byte) message;
        return byteData;
    }

    public int message;

    public OneByteSystemMessage(int deltaTime, int message) {
        super(deltaTime, null);
    }
}
