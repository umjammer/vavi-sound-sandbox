package unknown.sound.midi.track;

import unknown.sound.Message;


public class ControlChangeMessage extends ChannelMessage {
    protected void setControlNo(int control) {
        messageControlNo = control;
    }

    public int getControlNo() {
        return messageControlNo;
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
        byteData[index] = (byte) getControlNo();
        byteData[index + 1] = (byte) getData();
        return byteData;
    }

    private int messageControlNo;
    private int messageControlData;
    public static final int LENGTH = 2;

    public ControlChangeMessage(int deltaTime, int statusByte, byte[] data) {
        super(deltaTime, statusByte, data);
        setControlNo(data[0]);
        setData(data[1]);
    }

    public ControlChangeMessage(int deltaTime, int statusByte, int controlNo,
                                int data) {
        setDeltaTime(deltaTime);
        setStatus(statusByte);
        setControlNo(controlNo);
        setData(data);
    }
}
