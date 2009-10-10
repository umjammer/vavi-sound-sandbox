package unknown.sound.mfi.info;

import unknown.sound.Message;


public class InformationChunkHeader extends Message {
    public void setMelodyLength(int il) {
        melodyDataLength = il;
    }

    public int getMelodyLength() {
        return melodyDataLength;
    }

    public void setInformationLength(int il) {
        informationDataLength = il;
    }

    public int getInfomaitonLength() {
        return informationDataLength;
    }

    public void setStartTrack(int track) {
        informationStartTrack = track;
    }

    public int getStartTrack() {
        return informationStartTrack;
    }

    public void setEndTrack(int track) {
        informationEndTrack = track;
    }

    public int getEndTrack() {
        return informationEndTrack;
    }

    public void setTrackCount(int count) {
        informationTrackCount = count;
    }

    public int getTrackCount() {
        return informationTrackCount;
    }

    public byte[] toBytes() {
        byte[] bytes = {
                           109, 101, 108, 111, (byte) (getMelodyLength() >> 24),
                           (byte) (getMelodyLength() >> 16),
                           (byte) (getMelodyLength() >> 8),
                           (byte) getMelodyLength(),
                           (byte) (getInfomaitonLength() >> 8),
                           (byte) getInfomaitonLength(), (byte) getStartTrack(),
                           (byte) getEndTrack(), (byte) getTrackCount()
                       };
        return bytes;
    }

    private int melodyDataLength;
    private int informationDataLength;
    private int informationStartTrack;
    private int informationEndTrack;
    private int informationTrackCount;

    public InformationChunkHeader(int melodyLength, int informationLength,
                                 int start, int end, int count) {
        setMelodyLength(melodyLength);
        setInformationLength(informationLength);
        setStartTrack(start);
        setEndTrack(end);
        setTrackCount(count);
    }
}

/* */
