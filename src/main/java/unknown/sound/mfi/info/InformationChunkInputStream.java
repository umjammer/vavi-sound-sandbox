package unknown.sound.mfi.info;

import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;

import unknown.sound.Message;
import unknown.sound.mfi.MLDChunkInputStream;


public class InformationChunkInputStream extends MLDChunkInputStream {
    public void setMelodyLength(long ml) {
        melodyDataLength = ml;
    }

    public long getMelodyLength() {
        return melodyDataLength;
    }

    public void setStartTrack(int track) {
        melodyStartTrack = track;
    }

    public int getStartTrack() {
        return melodyStartTrack;
    }

    public void setEndTrack(int track) {
        melodyEndTrack = track;
    }

    public int getEndTrack() {
        return melodyEndTrack;
    }

    public void setTrackCount(int count) {
        melodyTrackCount = count;
    }

    public int getTrackCount() {
        return melodyTrackCount;
    }

    @Override
    public Message readMessage()
        throws InvalidMidiDataException, IOException {
        SourceMessage message = null;
        byte[] tagBytes = readMessageBytes(4);
        String tagStr = new String(tagBytes);
        byte[] messageByteData = readMessageBytes(readShort());
        if (tagStr.equals("sorc")) {
            message = new SourceMessage(messageByteData);
        }
        return message;
    }

    @Override
    public Message getChunkHeader() {
        return new InformationChunkHeader((int) getMelodyLength(),
                                         getDataByteArray().length,
                                         getStartTrack(), getEndTrack(),
                                         getTrackCount());
    }

    private long melodyDataLength;
    private int melodyStartTrack;
    private int melodyEndTrack;
    private int melodyTrackCount;
    public static final String TAG = "melo";

    public InformationChunkInputStream(int melodyDataLength, byte[] dataBytes)
        throws InvalidMidiDataException, IOException {
        super("melo", dataBytes);
        setMelodyLength(melodyDataLength);
        setStartTrack(read());
        setEndTrack(read());
        setTrackCount(read());
    }
}

/* */
