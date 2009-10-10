package unknown.sound.midi.header;

import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;

import unknown.sound.Message;
import unknown.sound.midi.MIDIChunkInputStream;


public class HeaderChunkInputStream extends MIDIChunkInputStream {
    public Message readMessage()
        throws InvalidMidiDataException, IOException {
        byte[] dataBytes = new byte[6];
        try {
            readMessageBytes(dataBytes);
        } catch (InvalidMidiDataException _ex) {
            throw new InvalidMidiDataException("header");
        }
        return new HeaderMessage(dataBytes);
    }

    public Message getChunkHeader() {
        return new HeaderChunkHeader(getDataByteArray().length);
    }

    public static final String MYTAG = "MThd";

    public HeaderChunkInputStream(byte[] dataBytes) throws InvalidMidiDataException {
        super(MYTAG, dataBytes);

        if (dataBytes.length != 6) {
            throw new InvalidMidiDataException("header");
        }
    }
}
