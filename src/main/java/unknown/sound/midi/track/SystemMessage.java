package unknown.sound.midi.track;

public class SystemMessage extends TrackMessage {
    public SystemMessage(int deltaTime, byte[] data) {
        super(deltaTime, data);
    }

    protected SystemMessage() {
    }
}
