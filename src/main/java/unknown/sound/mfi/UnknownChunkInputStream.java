package unknown.sound.mfi;

import unknown.sound.Message;


public class UnknownChunkInputStream extends MLDChunkInputStream {
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

/* */
