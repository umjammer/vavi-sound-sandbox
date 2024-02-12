package unknown.sound.midi.track;

public class EndOfTrackMessage extends MetaEventMessage {
    @Override
    public byte[] toBytes() {
        byte[] byteData = MetaEventMessage.arrayCopy(super.toBytes(), 0);
        return byteData;
    }

    public EndOfTrackMessage(int deltaTime, int dataType, byte[] data) {
        super(deltaTime, dataType, data);
    }

    public EndOfTrackMessage(int deltaTime) {
        setDeltaTime(deltaTime);
        setDataType(47);
    }
}
