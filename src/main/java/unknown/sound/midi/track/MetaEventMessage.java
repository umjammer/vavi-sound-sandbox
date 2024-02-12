package unknown.sound.midi.track;

import unknown.sound.Message;
import unknown.sound.midi.DeltaTimedMessage;


public abstract class MetaEventMessage extends TrackMessage {
    protected void setDataType(int type) {
        metaEventDataType = type;
    }

    public int getDataType() {
        return metaEventDataType;
    }

    @Override
    public byte[] toBytes() {
        byte[] byteData = Message.arrayCopy(super.toBytes(), 2);
        int index = byteData.length - 1 - 1;
        byteData[index] = -1;
        byteData[index + 1] = (byte) getDataType();
        return byteData;
    }

    public static byte[] arrayCopy(byte[] src, int dataLength) {
        byte[] srcLengthByte = DeltaTimedMessage.getDataAsBytes(dataLength);
        byte[] copyByte = Message.arrayCopy(src,
                                            srcLengthByte.length + dataLength);
        System.arraycopy(srcLengthByte, 0, copyByte, src.length,
                         srcLengthByte.length);
        return copyByte;
    }

    private int metaEventDataType;

    public MetaEventMessage(int deltaTime, int dataType, byte[] data) {
        super(deltaTime, data);
        setDataType(dataType);
    }

    protected MetaEventMessage() {
    }
}

/* */
