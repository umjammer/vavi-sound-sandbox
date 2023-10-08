package unknown.sound.mfi.track;

import unknown.sound.Message;


public class TrackChunkHeader extends Message {
    public void setDataLength(int dataLength) {
        messageDataLength = dataLength;
    }

    public int getDataLength() {
        return messageDataLength;
    }

    @Override
    public byte[] toBytes() {
        byte[] bytes = {
                           116, 114, 97, 99, (byte) (getDataLength() >> 24),
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
