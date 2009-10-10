package unknown.sound.midi.track;

import unknown.sound.Message;


public class TrackChunkHeader extends Message {
    public void setDataLength(int dataLength) {
        messageDataLength = dataLength;
    }

    public int getDataLength() {
        return messageDataLength;
    }

    public byte[] toBytes() {
        byte[] bytes = {
                           77, 84, 114, 107, (byte) (getDataLength() >> 24),
                           (byte) (getDataLength() >> 16),
                           (byte) (getDataLength() >> 8), (byte) getDataLength()
                       };
        return bytes;
    }

    protected int messageDataLength;

    public TrackChunkHeader(int dataLength) {
        setDataLength(dataLength);
    }
}
