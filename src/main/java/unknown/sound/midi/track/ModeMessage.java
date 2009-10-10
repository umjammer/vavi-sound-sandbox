package unknown.sound.midi.track;

import unknown.sound.Message;


public class ModeMessage extends ChannelMessage {
    protected void setMode(int mode) {
        messageMode = mode;
    }

    public int getMode() {
        return messageMode;
    }

    protected void setData(int data) {
        messageControlData = data;
    }

    protected int getData() {
        return messageControlData;
    }

    public byte[] toBytes() {
        byte[] byteData = Message.arrayCopy(super.toBytes(), 2);
        int index = byteData.length - 2;
        byteData[index] = (byte) getMode();
        byteData[index + 1] = (byte) getData();
        return byteData;
    }

    private int messageMode;
    private int messageControlData;
    public static final int LENGTH = 2;

    public ModeMessage(int deltaTime, int statusByte, byte[] data) {
        super(deltaTime, statusByte, data);
        setMode(data[0]);
        setData(data[1]);
    }

    public ModeMessage(int deltaTime, int statusByte, int mode, int data) {
        setDeltaTime(deltaTime);
        setStatus(statusByte);
        setMode(mode);
        setData(data);
    }
}

/* */
