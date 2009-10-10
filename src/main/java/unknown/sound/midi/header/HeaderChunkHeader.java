package unknown.sound.midi.header;

import unknown.sound.Message;


public class HeaderChunkHeader extends Message {
    public void setDataLength(int dataLength) {
        messageDataLength = dataLength;
    }

    public int getDataLength() {
        return messageDataLength;
    }

    public byte[] toBytes() {
        byte[] bytes = {
                           77, 84, 104, 100, (byte) (getDataLength() >> 24),
                           (byte) (getDataLength() >> 16),
                           (byte) (getDataLength() >> 8), (byte) getDataLength()
                       };
        return bytes;
    }

    protected int messageDataLength;

    public HeaderChunkHeader(int dataLength) {
        setDataLength(dataLength);
    }
}
