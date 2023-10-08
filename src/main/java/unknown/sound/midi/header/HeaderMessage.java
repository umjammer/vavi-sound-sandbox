package unknown.sound.midi.header;

import unknown.sound.Message;


public class HeaderMessage extends Message {
    protected void setFormatType(int type) {
        headerFormatType = type;
    }

    public int getFormatType() {
        return headerFormatType;
    }

    protected void setTrackCount(int count) {
        headerTrackCount = count;
    }

    public int getTrackCount() {
        return headerTrackCount;
    }

    protected void setResolution(int resolution) {
        headerResolution = resolution;
    }

    public int getResolution() {
        return headerResolution;
    }

    @Override
    public byte[] toBytes() {
        byte[] bytes = {
                           (byte) (getFormatType() >> 8), (byte) getFormatType(),
                           (byte) (getTrackCount() >> 8), (byte) getTrackCount(),
                           (byte) (getResolution() >> 8), (byte) getResolution()
                       };
        return bytes;
    }

    protected int headerFormatType;
    protected int headerTrackCount;
    protected int headerResolution;

    public HeaderMessage(byte[] dataBytes) {
        super(dataBytes);
        setFormatType((dataBytes[0] << 8) + (dataBytes[1] & 0xff));
        setTrackCount((dataBytes[2] << 8) + (dataBytes[3] & 0xff));
        setResolution((dataBytes[4] << 8) + (dataBytes[5] & 0xff));
    }

    public HeaderMessage(int formatType, int trackCount, int resolution) {
        setFormatType(formatType);
        setTrackCount(trackCount);
        setResolution(resolution);
    }
}

/* */
