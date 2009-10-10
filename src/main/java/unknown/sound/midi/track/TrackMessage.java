package unknown.sound.midi.track;

import unknown.sound.midi.DeltaTimedMessage;

public abstract class TrackMessage extends DeltaTimedMessage {

    public TrackMessage(int deltaTime, byte[] data) {
        super(deltaTime, data);
    }

    protected TrackMessage() {}
}

/* */
