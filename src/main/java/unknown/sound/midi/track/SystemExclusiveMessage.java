package unknown.sound.midi.track;

import unknown.sound.Message;
import unknown.sound.midi.DeltaTimedMessage;


public class SystemExclusiveMessage extends TrackMessage {
    public byte[] toBytes() {
        byte[] src = getDataBytes();
        byte[] dataLength = DeltaTimedMessage.getDataAsBytes(src.length);
        byte[] byteData = Message.arrayCopy(super.toBytes(),
                                            1 + dataLength.length + src.length);
        int index = byteData.length - src.length - dataLength.length - 1;
        byteData[index] = -16;
        System.arraycopy(dataLength, 0, byteData, index + 1, dataLength.length);
        if (src.length != 0) {
            System.arraycopy(src, 0, byteData, index + 1 + dataLength.length,
                             src.length);
        }
        return byteData;
    }

    public SystemExclusiveMessage(int deltaTime, byte[] data) {
        super(deltaTime, data);
    }
}
