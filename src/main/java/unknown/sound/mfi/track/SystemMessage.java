package unknown.sound.mfi.track;

public abstract class SystemMessage extends TrackMessage {
    public SystemMessage(int deltaTime) {
        super(deltaTime);
    }

    protected SystemMessage() {
    }
}
