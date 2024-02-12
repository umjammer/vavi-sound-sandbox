package unknown.sound.midi.track;

import unknown.sound.Message;


public class PolyphonicKeyPressureMessage extends ChannelMessage {
    protected void setNote(int note) {
        messageNote = note;
    }

    public int getNote() {
        return messageNote;
    }

    protected void setPressure(int velocity) {
        messagePressure = velocity;
    }

    public int getPressure() {
        return messagePressure;
    }

    @Override
    public byte[] toBytes() {
        byte[] byteData = Message.arrayCopy(super.toBytes(), 2);
        int index = byteData.length - 2;
        byteData[index] = (byte) getNote();
        byteData[index + 1] = (byte) getPressure();
        return byteData;
    }

    private int messageNote;
    private int messagePressure;
    public static final int LENGTH = 2;

    public PolyphonicKeyPressureMessage(int deltaTime, int statusByte,
                                        byte[] data) {
        super(deltaTime, statusByte, data);
        setNote(data[0]);
        setPressure(data[1]);
    }

    public PolyphonicKeyPressureMessage(int deltaTime, int statusByte,
                                        int note, int pressure) {
        setDeltaTime(deltaTime);
        setStatus(statusByte);
        setNote(note);
        setPressure(pressure);
    }
}

/* */
