package unknown.sound.midi.track;

import unknown.sound.Message;


public class NoteOnMessage extends ChannelMessage {
    protected void setNote(int note) {
        messageNote = note;
    }

    public int getNote() {
        return messageNote;
    }

    protected void setVelocity(int velocity) {
        messageVelocity = velocity;
    }

    public int getVelocity() {
        return messageVelocity;
    }

    @Override
    public byte[] toBytes() {
        byte[] byteData = Message.arrayCopy(super.toBytes(), 2);
        int index = byteData.length - 2;
        byteData[index] = (byte) getNote();
        byteData[index + 1] = (byte) getVelocity();
        return byteData;
    }

    private int messageNote;
    private int messageVelocity;
    public static final int LENGTH = 2;

    public NoteOnMessage(int deltaTime, int statusByte, byte[] data) {
        super(deltaTime, statusByte, data);
        setNote(data[0]);
        setVelocity(data[1]);
    }

    public NoteOnMessage(int deltaTime, int statusByte, int note, int velocity) {
        setDeltaTime(deltaTime);
        setStatus(statusByte);
        setNote(note);
        setVelocity(velocity);
    }
}
