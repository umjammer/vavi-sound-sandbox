package unknown.sound.midi;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;

import javax.sound.midi.InvalidMidiDataException;

import unknown.sound.midi.header.HeaderChunkInputStream;
import unknown.sound.midi.track.TrackChunkInputStream;


public class MIDIInputStream extends DataInputStream {
    public MIDIInputStream(InputStream stream) {
        super(stream);
    }

    public MIDIChunkInputStream readMIDIChunk()
        throws IOException, InvalidMidiDataException {
        byte[] tagBytes = new byte[4];
        int readLength = read(tagBytes);

        if (readLength == -1) {
            throw new EOFException();
        }
        if (readLength != tagBytes.length) {
            throw new InvalidMidiDataException("midi format");
        }

        int dataLength = readInt();
        String tagStr = new String(tagBytes);
        byte[] dataBytes = (byte[]) Array.newInstance(Byte.TYPE, dataLength);
        read(dataBytes);

        MIDIChunkInputStream chunk;
        if (tagStr.equals(HeaderChunkInputStream.MYTAG)) {
            chunk = new HeaderChunkInputStream(dataBytes);
        } else {
            if (tagStr.equals(TrackChunkInputStream.MYTAG)) {
                chunk = new TrackChunkInputStream(dataBytes);
            } else {
                chunk = new UnknownChunkInputStream(tagStr, dataBytes);
            }
        }
        return chunk;
    }
}

/* */
