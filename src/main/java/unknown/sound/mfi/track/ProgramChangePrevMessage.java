package unknown.sound.mfi.track;

public class ProgramChangePrevMessage extends SystemMessage {
    public void setProgram(int program) {
        messageProgram = program;
    }

    public int getProgram() {
        return messageProgram;
    }

    public void setChannel(int channel) {
        messageChannel = channel;
    }

    public int getChannel() {
        return messageChannel;
    }

    public int getChannelProgram() {
        return ((getChannel() << 6) & 0xc0) + (getProgram() & 0x3f);
    }

    public byte[] toBytes() {
        byte[] bytes = {
                           (byte) getDeltaTime(), -1, -32,
                           (byte) getChannelProgram()
                       };
        return bytes;
    }

    private int messageProgram;
    private int messageChannel;

    public ProgramChangePrevMessage(int deltaTime, int channelProgram) {
        this(deltaTime, (channelProgram >> 6) & 3, channelProgram & 0x3f);
    }

    public ProgramChangePrevMessage(int deltaTime, int channel, int program) {
        super(deltaTime);
        setProgram(program);
        setChannel(channel);
    }
}
