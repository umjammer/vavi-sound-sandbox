package unknown.sound.mfi;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.midi.InvalidMidiDataException;

import unknown.sound.Message;
import unknown.sound.mfi.info.InformationChunkInputStream;
import unknown.sound.mfi.track.TrackChunkInputStream;


public class MLDInputStream extends DataInputStream {
    public MLDInputStream(InputStream stream) {
        super(stream);
        mldMelodyDataLength = -1;
    }

    private int mldMelodyDataLength;

    public int getMelodyDataLength() {
        return mldMelodyDataLength;
    }

    protected void setMelodyDataLength(int mldLength) {
        mldMelodyDataLength = mldLength;
    }

    public MLDChunkInputStream readMLDChunk()
        throws IOException, InvalidMidiDataException {
        byte[] tagBytes = new byte[4];
        int readLength = read(tagBytes);

        if (readLength == -1) {
            throw new EOFException();
        }
        if (readLength != tagBytes.length) {
            throw new InvalidMidiDataException("mld format");
        }

        String tagStr = new String(tagBytes);
        MLDChunkInputStream chunk;
        if (tagStr.equals("melo")) {
            setMelodyDataLength(readInt());

            byte[] informationData = Message.getByteArray(readShort());
            read(informationData);
            chunk = new InformationChunkInputStream(getMelodyDataLength(),
                                                   informationData);
        } else {
            if (tagStr.equals("trac")) {
                int trackDataLength = readShort();
                byte[] trackData = Message.getByteArray(trackDataLength);
                read(trackData);
                chunk = new TrackChunkInputStream(trackData);
            } else {
                int dataLength = readShort();
                byte[] data = Message.getByteArray(dataLength);
                read(data);
                chunk = new UnknownChunkInputStream(tagStr, data);
            }
        }

        return chunk;
    }
}
