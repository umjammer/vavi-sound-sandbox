package unknown.sound.mfi.track;

import unknown.sound.Message;


public abstract class TrackMessage extends Message {
    public void setDeltaTime(int deltaTime) {
        messageDeltaTime = deltaTime;
    }

    public int getDeltaTime() {
        return messageDeltaTime;
    }

    public void setAbsoluteTime(long abs) {
        messageAbsoluteTime = abs;
    }

    public long getAbsoluteTime() {
        return messageAbsoluteTime;
    }

    public long incAbsoluteTime(long absoluteTime) {
        setAbsoluteTime(getDeltaTime() + absoluteTime);
        return getAbsoluteTime();
    }

    @Override
    public abstract byte[] toBytes();

    private int messageDeltaTime;
    private long messageAbsoluteTime;

    public TrackMessage(int deltaTime) {
        setDeltaTime(deltaTime);
    }

    protected TrackMessage() {
    }
}
