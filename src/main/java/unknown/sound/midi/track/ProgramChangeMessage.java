package unknown.sound.midi.track;

import unknown.sound.Message;


public class ProgramChangeMessage extends ChannelMessage {
    protected void setProgram(int program) {
        messageProgram = program;
    }

    public int getProgram() {
        return messageProgram;
    }

    public byte[] toBytes() {
        byte[] byteData = Message.arrayCopy(super.toBytes(), 1);
        int index = byteData.length - 1;
        byteData[index] = (byte) getProgram();
        return byteData;
    }

    private int messageProgram;
    public static final int LENGTH = 1;

    public ProgramChangeMessage(int deltaTime, int statusByte, byte[] data) {
        super(deltaTime, statusByte, data);
        setProgram(data[0]);
    }

    public ProgramChangeMessage(int deltaTime, int statusByte, int program) {
        setDeltaTime(deltaTime);
        setStatus(statusByte);
        setProgram(program);
    }
}
