package unknown.sound.midi.track;

public class SequenceNoMessage extends MetaEventMessage {
    protected void setSequence(int sequence) {
        messageSequenceNo = sequence;
    }

    public int getSequence() {
        return messageSequenceNo;
    }

    @Override
    public byte[] toBytes() {
        byte[] byteData = MetaEventMessage.arrayCopy(super.toBytes(), 2);
        int index = byteData.length - 2;
        byteData[index] = (byte) (getSequence() >> 8);
        byteData[index + 1] = (byte) getSequence();
        return byteData;
    }

    private int messageSequenceNo;
    public static final int LENGTH = 2;

    public SequenceNoMessage(int deltaTime, int dataType, byte[] data) {
        super(deltaTime, dataType, data);
        setSequence((data[0] & 0xff) << (8 + (data[1] & 0xff)));
    }
}
