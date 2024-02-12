package unknown.sound.midi;

import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;

import unknown.sound.ChunkInputStream;
import unknown.sound.Message;


public abstract class MIDIChunkInputStream extends ChunkInputStream {
    @Override
    public abstract Message readMessage()
        throws InvalidMidiDataException, IOException;

    public MIDIChunkInputStream(String tag, byte[] dataBytes) {
        super(tag, dataBytes);
    }
}
