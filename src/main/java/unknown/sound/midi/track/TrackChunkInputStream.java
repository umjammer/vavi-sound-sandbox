package unknown.sound.midi.track;

import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;

import unknown.sound.Message;
import unknown.sound.midi.MIDIChunkInputStream;


public class TrackChunkInputStream extends MIDIChunkInputStream {
    private int getPreviousStatusByte() {
        return previousStatusByteTempo;
    }

    private void setPreviousStatusByte(int statusByte) {
        previousStatusByteTempo = statusByte;
    }

    private void setAbsoluteTime(long time) {
        trackAbsoluteTime = time;
    }

    public long getAbsoluteTime() {
        return trackAbsoluteTime;
    }

    @Override
    public Message readMessage()
        throws InvalidMidiDataException, IOException {
        try {
            int deltaTime = readDeltaTime();
            mark(10);

            int statusByte = read();
            if (statusByte < 128) {
                reset();
                statusByte = getPreviousStatusByte();
            }
            setPreviousStatusByte(statusByte);

            int upperStatus = (statusByte >> 4) & 0xf;
            TrackMessage message;
label0:
            switch (upperStatus) {
            case 8: //
                message = new NoteOffMessage(deltaTime, statusByte,
                                             readMessageBytes(2));
                break;
            case 9: //
                message = new NoteOnMessage(deltaTime, statusByte,
                                            readMessageBytes(2));
                break;
            case 10: //
                message = new PolyphonicKeyPressureMessage(deltaTime,
                                                           statusByte,
                                                           readMessageBytes(2));
                break;
            case 11: //
                if ((statusByte & 0xf) < 120) {
                    message = new ControlChangeMessage(deltaTime, statusByte,
                                                       readMessageBytes(2));
                } else {
                    message = new ModeMessage(deltaTime, statusByte,
                                              readMessageBytes(2));
                }
                break;
            case 12: //
                message = new ProgramChangeMessage(deltaTime, statusByte,
                                                   readMessageBytes(1));
                break;
            case 13: //
                message = new ChannelPressureMessage(deltaTime, statusByte,
                                                     readMessageBytes(1));
                break;
            case 14: //
                message = new PitchBendChangeMessage(deltaTime, statusByte,
                                                     readMessageBytes(2));
                break;
            case 15: //
                switch (statusByte & 0xf) {
                case 0: //
                case 7: //

                    int dataLength = readDeltaTime();
                    message = new SystemExclusiveMessage(deltaTime,
                                                         readMessageBytes(dataLength));
                    break label0;
                case 1: //
                    message = new QuaterFrameMessage(deltaTime,
                                                     readMessageBytes(1));
                    break label0;
                case 2: //
                    message = new SongPointerMessage(deltaTime,
                                                     readMessageBytes(2));
                    break label0;
                case 3: //
                    message = new SongSelectMessage(deltaTime,
                                                    readMessageBytes(1));
                    break label0;
                case 15: //

                    int dataType = read();
                    int dataLength2 = readDeltaTime();
                    message = switch (dataType) {
                        case 0 -> //
                                new SequenceNoMessage(deltaTime, dataType,
                                        readMessageBytes(dataLength2));
                        case 1 -> //
                                new TextMessage(deltaTime, dataType,
                                        readMessageBytes(dataLength2));
                        case 2 -> //
                                new RightMessage(deltaTime, dataType,
                                        readMessageBytes(dataLength2));
                        case 3 -> //
                                new SequenceTrackNameMessage(deltaTime,
                                        dataType,
                                        readMessageBytes(dataLength2));
                        case 47 -> //
                                new EndOfTrackMessage(deltaTime, dataType,
                                        readMessageBytes(dataLength2));
                        case 81 -> //
                                new SetTempoMessage(deltaTime, dataType,
                                        readMessageBytes(dataLength2));
                        default -> new UnknownMessage(deltaTime, dataType,
                                readMessageBytes(dataLength2));
                    };
                    break;
                case 4: //
                case 5: //
                case 6: //
                case 8: //
                case 9: //
                case 10: //
                case 11: //
                case 12: //
                case 13: //
                case 14: //
                default:
                    message = new OneByteSystemMessage(deltaTime, statusByte);
                    break;
                }
                break;
            default:
                throw new InvalidMidiDataException("track");
            }
            setAbsoluteTime(message.incAbsoluteTime(getAbsoluteTime()));
            return message;
        } catch (InvalidMidiDataException e) {
            throw new InvalidMidiDataException("track");
        }
    }

    @Override
    public Message getChunkHeader() {
        return new TrackChunkHeader(getDataByteArray().length);
    }

    private int readDeltaTime() throws InvalidMidiDataException, IOException {
        int deltaTime = 0;
        int dataIndex = 0;
        int nextData;
        do {
            nextData = read();
            if (nextData == -1) {
                throw new InvalidMidiDataException("track");
            }
            deltaTime = (deltaTime << 7) + (nextData & 0x7f);
        } while (nextData >= 128);
        if (dataIndex >= 4) {
            throw new InvalidMidiDataException("track");
        } else {
            dataIndex++;
            return deltaTime;
        }
    }

    private int previousStatusByteTempo;
    private long trackAbsoluteTime;
    public static String MYTAG = "MTrk";

    public TrackChunkInputStream(byte[] dataBytes) {
        super(MYTAG, dataBytes);
        previousStatusByteTempo = 128;
        trackAbsoluteTime = 0L;
    }
}

/* */
