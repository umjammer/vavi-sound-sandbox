package unknown.sound;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;


public abstract class ChunkInputStream extends DataInputStream {
    public ChunkInputStream(String tag, byte[] dataBytes) {
        super(new ByteArrayInputStream(dataBytes));
        setDataByteArray(dataBytes);
        setTag(tag);
    }

    public String getTag() {
        return chunkTag;
    }

    protected void setTag(String tag) {
        chunkTag = tag;
    }

    public byte[] getDataByteArray() {
        return chunkDataBytes;
    }

    protected void setDataByteArray(byte[] bytes) {
        chunkDataBytes = bytes;
    }

    public abstract Message readMessage()
        throws InvalidMidiDataException, IOException;

    public abstract Message getChunkHeader();

    public void readMessageBytes(byte[] dataBytes)
        throws InvalidMidiDataException, IOException {
        int readLength = super.read(dataBytes);
        if (readLength == -1) {
            throw new EOFException();
        }
        if (readLength != dataBytes.length) {
            throw new InvalidMidiDataException("format");
        } else {
        }
    }

    public byte[] readMessageBytes(int byteLength)
        throws InvalidMidiDataException, IOException {
        byte[] dataByte = Message.getByteArray(byteLength);
        readMessageBytes(dataByte);
        return dataByte;
    }

    private String chunkTag;
    private byte[] chunkDataBytes;
}
