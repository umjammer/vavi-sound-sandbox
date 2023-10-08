package unknown.sound.midi;

import unknown.sound.Message;


public abstract class DeltaTimedMessage extends Message {
    public int getDeltaTime() {
        return messageDeltaTime;
    }

    protected void setDeltaTime(int deltaTime) {
        messageDeltaTime = deltaTime;
    }

    public byte[] getDeltaTimeAsBytes() {
        return getDataAsBytes(getDeltaTime());
    }

    public long getAbsoluteTime() {
        return messageAbsoluteTime;
    }

    public void setAbsoluteTime(long time) {
        messageAbsoluteTime = time;
    }

    public long incAbsoluteTime(long absTime) {
        messageAbsoluteTime = absTime + getDeltaTime();
        return messageAbsoluteTime;
    }

    public static byte[] getDataAsBytes(int deltaTime) {
        byte[] bytes;
        if (deltaTime < 128) {
            byte[] array = { (byte) deltaTime };
            bytes = array;
        } else if (deltaTime < 16384) {
            byte[] array = {
                               (byte) ((deltaTime >> 7) | 0x80),
                               (byte) (deltaTime & 0x7f)
                           };
            bytes = array;
        } else if (deltaTime < 0x200000) {
            byte[] array = {
                               (byte) ((deltaTime >> 14) | 0x80),
                               (byte) ((deltaTime >> 7) | 0x80),
                               (byte) (deltaTime & 0x7f)
                           };
            bytes = array;
        } else if (deltaTime < 0x10000000) {
            byte[] array = {
                               (byte) ((deltaTime >> 21) | 0x80),
                               (byte) ((deltaTime >> 14) | 0x80),
                               (byte) ((deltaTime >> 7) | 0x80),
                               (byte) (deltaTime & 0x7f)
                           };
            bytes = array;
        } else {
            byte[] array = {
                               (byte) ((deltaTime >> 28) | 0x80),
                               (byte) ((deltaTime >> 21) | 0x80),
                               (byte) ((deltaTime >> 14) | 0x80),
                               (byte) ((deltaTime >> 7) | 0x80),
                               (byte) (deltaTime | 0x80)
                           };
            bytes = array;
        }
        return bytes;
    }

    @Override
    public byte[] toBytes() {
        return getDeltaTimeAsBytes();
    }

    protected int messageDeltaTime;
    protected long messageAbsoluteTime;

    public DeltaTimedMessage(int deltaTime, byte[] dataBytes) {
        super(dataBytes);
        setDeltaTime(deltaTime);
    }

    protected DeltaTimedMessage() {
    }
}

/* */
