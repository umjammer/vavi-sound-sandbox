package unknown.sound.mfi.track;

public class NoteMessage extends TrackMessage {
    public int getChannel() {
        return messageChannel;
    }

    public void setChannel(int channel) {
        messageChannel = channel;
    }

    public int getNote() {
        return messageNote;
    }

    public void setNote(int note) {
        messageNote = note;
    }

    public int getSoundLength() {
        return messageSoundLength;
    }

    public void setSoundLength(int soundLength) {
        messageSoundLength = soundLength;
    }

    public int getChannelNote() {
        return ((getChannel() << 6) & 0xc0) + (getNote() & 0x3f);
    }

    public byte[] toBytes() {
        byte[] bytes = {
                           (byte) getDeltaTime(), (byte) getChannelNote(),
                           (byte) getSoundLength()
                       };
        return bytes;
    }

    private int messageChannel;
    private int messageNote;
    private int messageSoundLength;

    public NoteMessage(int deltaTime, int channelNote, int soundLength) {
        this(deltaTime, (channelNote & 0xc0) >> 6, channelNote & 0x3f,
             soundLength);
    }

    public NoteMessage(int deltaTime, int channel, int note, int soundLength) {
        super(deltaTime);
        setChannel(channel);
        setNote(note);
        setSoundLength(soundLength);
    }
}
