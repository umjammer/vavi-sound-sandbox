package unknown.sound.midi.track;

import unknown.sound.Message;


public abstract class ChannelMessage extends TrackMessage {
    protected void setStatus(int status) {
        channel = status & 0xf;
        messageType = (status >> 4) & 0xf;
    }

    public int getChunnel() {
        return channel;
    }

    public int getStatus() {
        return messageType;
    }

    @Override
    public byte[] toBytes() {
        byte[] byteData = Message.arrayCopy(super.toBytes(), 1);
        byteData[byteData.length - 1] = (byte) (getChunnel() +
                                        ((getStatus() << 4) & 0xf0));
        return byteData;
    }

    private int channel;
    private int messageType;

    public ChannelMessage(int deltaTime, int statusByte, byte[] data) {
        super(deltaTime, data);
        setStatus(statusByte);
    }

    protected ChannelMessage() {
    }
}
