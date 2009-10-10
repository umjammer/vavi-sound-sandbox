package unknown.sound.mfi;

import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;

import unknown.sound.ChunkInputStream;
import unknown.sound.Message;


public abstract class MLDChunkInputStream extends ChunkInputStream {
    public abstract Message readMessage()
        throws InvalidMidiDataException, IOException;

    public abstract Message getChunkHeader();

    public MLDChunkInputStream(String tag, byte[] dataBytes) {
        super(tag, dataBytes);
    }
}

/* */
