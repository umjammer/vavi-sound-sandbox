package unknown.sound.mfi.track;


/**
 * ボリューム変更
 * delta, 0xff, 0xe2,
 */
public class SoundMessage extends SystemMessage {
    public void setSound(int sound) {
        messageSound = sound;
    }

    public int getSound() {
        return messageSound;
    }

    public void setChannel(int channel) {
        messageChannel = channel;
    }

    public int getChannel() {
        return messageChannel;
    }

    public int getChannelProgram() {
        return ((getChannel() << 6) & 0xc0) | (getSound() & 0x3f);
    }

    public byte[] toBytes() {
        byte[] bytes = {
                           (byte) getDeltaTime(), -1, -30,
                           (byte) getChannelProgram()
                       };
        return bytes;
    }

    private int messageSound;
    private int messageChannel;

    public SoundMessage(int deltaTime, int channelSound) {
        this(deltaTime, (channelSound >> 6) & 3, channelSound & 0x3f);
    }

    public SoundMessage(int deltaTime, int channel, int sound) {
        super(deltaTime);
        setSound(sound);
        setChannel(channel);
    }
}
