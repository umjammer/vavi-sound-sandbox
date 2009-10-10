package unknown.sound.midi;

import unknown.sound.Message;


public class UnknownChunkInputStream extends MIDIChunkInputStream {
    public Message readMessage() {
        return new UnknownMessage(getDataByteArray());
    }

    public Message getChunkHeader() {
        return readMessage();
    }

    public UnknownChunkInputStream(String tag, byte[] dataBytes) {
        super(tag, dataBytes);
    }
}
