package unknown.sound.midi.track;

public class SequenceTrackNameMessage extends MetaEventMessage {
    protected void setTrackName(String trackName) {
        messageTrackName = trackName;
    }

    public String getTrackName() {
        return messageTrackName;
    }

    @Override
    public byte[] toBytes() {
        byte[] textData = getTrackName().getBytes();
        byte[] byteData = MetaEventMessage.arrayCopy(super.toBytes(),
                                                     textData.length);
        int index = byteData.length - textData.length;
        System.arraycopy(textData, 0, byteData, index, textData.length);
        return byteData;
    }

    private String messageTrackName;

    public SequenceTrackNameMessage(int deltaTime, int dataType, byte[] data) {
        super(deltaTime, dataType, data);
        setTrackName(new String(data));
    }

    public SequenceTrackNameMessage(int deltaTime, String trackName) {
        setDeltaTime(deltaTime);
        setTrackName(trackName);
        setDataType(3);
    }
}
