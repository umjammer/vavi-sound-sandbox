package unknown.sound.mfi.track;

import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;

import unknown.sound.Message;
import unknown.sound.mfi.MLDChunkInputStream;


public class TrackChunkInputStream extends MLDChunkInputStream {
    private void setAbsoluteTime(long abs) {
        trackAbsoluteTime = abs;
    }

    public long getAbsoluteTime() {
        return trackAbsoluteTime;
    }

    public Message readMessage()
        throws InvalidMidiDataException, IOException {
        int deltaTime = read();
        int statusByte = read();
        TrackMessage message;
        if (statusByte == 255) {
            int systemCode = read();
            switch (systemCode >> 4) {
            case 12: // '\f'

                int tempo = read();
                message = new TempoMessage(deltaTime, systemCode & 0xf, tempo);
                break;
            case 13: // '\r'

                int trackData = read();
                message = switch (systemCode & 0xf) {
                    case 0 -> // '\0'
                            new TrackBeginningMessage(deltaTime, trackData);
                    case 15 -> // '\017'
                            new TrackEndMessage(deltaTime, trackData);
                    default -> throw new InvalidMidiDataException("track");
                };
                break;
            case 14: // '\016'

                int channelData = read();
                message = switch (systemCode & 0xf) {
                    case 0 -> // '\0'
                            new ProgramChangePrevMessage(deltaTime,
                                    channelData);
                    case 1 -> // '\001'
                            new ProgramChangeNextMessage(deltaTime,
                                    channelData);
                    case 2 -> // '\002'
                            new SoundMessage(deltaTime, channelData);
                    default -> throw new InvalidMidiDataException("track");
                };
                break;
            case 15: // '\017'

                int dataLength = readShort();
                byte[] dataBytes = readMessageBytes(dataLength);
                message = new SpecialMessage(deltaTime, dataBytes);
                break;
            default:
                throw new InvalidMidiDataException("track");
            }
        } else {
            int soundLength = read();
            message = new NoteMessage(deltaTime, statusByte, soundLength);
        }
        setAbsoluteTime(message.incAbsoluteTime(getAbsoluteTime()));
        return message;
    }

    public Message getChunkHeader() {
        return new TrackChunkHeader(getDataByteArray().length);
    }

    private long trackAbsoluteTime;
    public static final String TAG = "trac";

    public TrackChunkInputStream(byte[] dataBytes)
        throws InvalidMidiDataException, IOException {
        super("trac", dataBytes);
        trackAbsoluteTime = 0L;
    }
}
