package unknown.sound.midi.track;

public class UnknownMessage extends MetaEventMessage {
    @Override
    public byte[] toBytes() {
        byte[] unknownData = getDataBytes();
        byte[] byteData = MetaEventMessage.arrayCopy(super.toBytes(),
                                                     unknownData.length);
        int index = byteData.length - unknownData.length;
        System.arraycopy(unknownData, 0, byteData, index, unknownData.length);
        return byteData;
    }

    public UnknownMessage(int deltaTime, int dataType, byte[] data) {
        super(deltaTime, dataType, data);
    }
}
