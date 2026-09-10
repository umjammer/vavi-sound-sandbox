/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.ump;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


/**
 * Universal MIDI Packet, the message container of MIDI 2.0.
 * <p>
 * A message is one to four 32 bit words, the first of which tells by its top nibble how many.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/09/11 nsano initial version <br>
 * @see "https://midi.org/universal-midi-packet-ump-and-midi-2-0-protocol-specification"
 */
public final class Ump {

//#region message type

    /** Utility, delta clockstamps and jitter reduction */
    public static final int UTILITY = 0x0;
    /** System Real Time and System Common */
    public static final int SYSTEM = 0x1;
    /** MIDI 1.0 Channel Voice, 7 bit resolution */
    public static final int MIDI1_CHANNEL_VOICE = 0x2;
    /** Data, that is System Exclusive in 7 bit */
    public static final int SYSEX7 = 0x3;
    /** MIDI 2.0 Channel Voice, 16 to 32 bit resolution */
    public static final int MIDI2_CHANNEL_VOICE = 0x4;
    /** Data, that is System Exclusive in 8 bit, and Mixed Data Set */
    public static final int DATA128 = 0x5;
    /** Flex Data, tempo, key signature, lyrics and other metadata */
    public static final int FLEX_DATA = 0xd;
    /** UMP Stream, endpoint and function block discovery, start and end of clip */
    public static final int STREAM = 0xf;

//#endregion

//#region status

    /** {@link #UTILITY} status */
    public static final int NOOP = 0x0;
    /** {@link #UTILITY} status */
    public static final int JR_CLOCK = 0x1;
    /** {@link #UTILITY} status */
    public static final int JR_TIMESTAMP = 0x2;
    /** {@link #UTILITY} status, Delta Clockstamp Ticks Per Quarter Note */
    public static final int DCTPQ = 0x3;
    /** {@link #UTILITY} status, Delta Clockstamp */
    public static final int DCS = 0x4;

    /** channel voice status */
    public static final int REGISTERED_PER_NOTE_CONTROLLER = 0x0;
    /** channel voice status */
    public static final int ASSIGNABLE_PER_NOTE_CONTROLLER = 0x1;
    /** channel voice status */
    public static final int REGISTERED_CONTROLLER = 0x2;
    /** channel voice status */
    public static final int ASSIGNABLE_CONTROLLER = 0x3;
    /** channel voice status */
    public static final int NOTE_OFF = 0x8;
    /** channel voice status */
    public static final int NOTE_ON = 0x9;
    /** channel voice status */
    public static final int POLY_PRESSURE = 0xa;
    /** channel voice status */
    public static final int CONTROL_CHANGE = 0xb;
    /** channel voice status */
    public static final int PROGRAM_CHANGE = 0xc;
    /** channel voice status */
    public static final int CHANNEL_PRESSURE = 0xd;
    /** channel voice status */
    public static final int PITCH_BEND = 0xe;

    /** {@link #FLEX_DATA} status bank */
    public static final int SETUP_AND_PERFORMANCE = 0x00;
    /** {@link #FLEX_DATA} status bank */
    public static final int METADATA_TEXT = 0x01;
    /** {@link #FLEX_DATA} status of {@link #SETUP_AND_PERFORMANCE} */
    public static final int SET_TEMPO = 0x00;

    /** {@link #STREAM} status */
    public static final int START_OF_CLIP = 0x20;
    /** {@link #STREAM} status */
    public static final int END_OF_CLIP = 0x21;

//#endregion

    /** number of 32 bit words a message takes, by message type */
    private static final int[] WORDS = {
        1, 1, 1, 2, 2, 4, 1, 1, 2, 2, 2, 3, 3, 4, 3, 4
    };

    private Ump() {
    }

    /** @param word the first word of a message */
    public static int messageType(int word) {
        return word >>> 28;
    }

    /**
     * How long the message starting with this word is.
     *
     * @param word the first word of a message
     * @return number of 32 bit words, 1 to 4
     */
    public static int words(int word) {
        return WORDS[messageType(word)];
    }

    /** @param word the first word of a message */
    public static int group(int word) {
        return (word >>> 24) & 0x0f;
    }

    /** the status nibble, of a {@link #UTILITY} or a channel voice message */
    public static int status(int word) {
        return (word >>> 20) & 0x0f;
    }

    /** the channel nibble, of a channel voice message */
    public static int channel(int word) {
        return (word >>> 16) & 0x0f;
    }

    /** the 20 bit payload of a {@link #UTILITY} message */
    public static int data(int word) {
        return word & 0xfffff;
    }

    /** the 10 bit status of a {@link #STREAM} message */
    public static int streamStatus(int word) {
        return (word >>> 16) & 0x3ff;
    }

    /** the form of a {@link #FLEX_DATA} message, 0 complete, 1 start, 2 continue, 3 end */
    public static int flexForm(int word) {
        return (word >>> 22) & 0x03;
    }

    /** the status bank of a {@link #FLEX_DATA} message */
    public static int flexStatusBank(int word) {
        return (word >>> 8) & 0xff;
    }

    /** the status of a {@link #FLEX_DATA} message, within its {@link #flexStatusBank(int)} */
    public static int flexStatus(int word) {
        return word & 0xff;
    }

    /** whether the message sets the tempo, whose value is then {@code words[1]} in 10 ns per quarter note */
    public static boolean isSetTempo(int[] words) {
        return messageType(words[0]) == FLEX_DATA &&
                flexStatusBank(words[0]) == SETUP_AND_PERFORMANCE &&
                flexStatus(words[0]) == SET_TEMPO;
    }

    /** whether the message carries a chunk of {@link #text(int[])} */
    public static boolean isText(int[] words) {
        return messageType(words[0]) == FLEX_DATA && flexStatusBank(words[0]) == METADATA_TEXT;
    }

    /**
     * The UTF-8 payload of a {@link #FLEX_DATA} message. A long text is split over several messages,
     * see {@link #flexForm(int)}, and those have to be concatenated by the caller.
     */
    public static String text(int[] words) {
        ByteBuffer buffer = ByteBuffer.allocate((words.length - 1) * Integer.BYTES);
        for (int i = 1; i < words.length; i++) {
            buffer.putInt(words[i]);
        }
        byte[] bytes = buffer.array();
        // the last message of a text is padded with NUL
        int length = bytes.length;
        while (length > 0 && bytes[length - 1] == 0) {
            length--;
        }
        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }

    /**
     * Down converts a {@link #MIDI2_CHANNEL_VOICE} message to the {@link #MIDI1_CHANNEL_VOICE}
     * messages that come closest to it, losing resolution on the way. Any other message is returned
     * as it is, and a MIDI 2.0 message with no MIDI 1.0 counterpart, a per note controller for
     * instance, becomes nothing at all.
     *
     * @param words one complete message
     * @return zero or more complete messages, one word each when a conversion took place
     */
    public static int[] toMidi1(int... words) {
        if (messageType(words[0]) != MIDI2_CHANNEL_VOICE) {
            return words;
        }
        int status = status(words[0]);
        int header = (MIDI1_CHANNEL_VOICE << 28) | (group(words[0]) << 24) | (status << 20) | (channel(words[0]) << 16);
        int byte2 = (words[0] >>> 8) & 0xff;
        int byte3 = words[0] & 0xff;
        int data = words[1];
        return switch (status) {
            case NOTE_OFF, NOTE_ON -> {
                // the top 16 bits are the velocity, the rest is the attribute value MIDI 1.0 has no room for
                int velocity = (data >>> 16) >>> 9;
                // MIDI 1.0 has no note on of velocity 0, that would be a note off
                yield new int[] {header | ((byte2 & 0x7f) << 8) | (status == NOTE_ON ? Math.max(velocity, 1) : velocity)};
            }
            // the note of a poly pressure and the index of a control change sit in the same byte
            case POLY_PRESSURE, CONTROL_CHANGE -> new int[] {header | ((byte2 & 0x7f) << 8) | (data >>> 25)};
            case CHANNEL_PRESSURE -> new int[] {header | ((data >>> 25) << 8)};
            case PITCH_BEND -> {
                int bend = data >>> 18; // 14 bit
                yield new int[] {header | ((bend & 0x7f) << 8) | (bend >> 7)};
            }
            case PROGRAM_CHANGE -> {
                int control = (MIDI1_CHANNEL_VOICE << 28) | (group(words[0]) << 24) |
                        (CONTROL_CHANGE << 20) | (channel(words[0]) << 16);
                int change = header | (((data >>> 24) & 0x7f) << 8);
                // the bank valid flag tells whether the bank select of the second word means anything
                yield (byte3 & 1) == 0 ? new int[] {change} : new int[] {
                        control | (0 << 8) | ((data >>> 8) & 0x7f),
                        control | (32 << 8) | (data & 0x7f),
                        change
                };
            }
            case REGISTERED_CONTROLLER, ASSIGNABLE_CONTROLLER -> {
                int control = (MIDI1_CHANNEL_VOICE << 28) | (group(words[0]) << 24) |
                        (CONTROL_CHANGE << 20) | (channel(words[0]) << 16);
                int bank = status == REGISTERED_CONTROLLER ? 101 : 99;
                int value = data >>> 18; // 14 bit
                yield new int[] {
                        control | (bank << 8) | (byte2 & 0x7f),
                        control | ((bank - 1) << 8) | (byte3 & 0x7f),
                        control | (6 << 8) | (value >> 7),
                        control | (38 << 8) | (value & 0x7f)
                };
            }
            // per note controllers, per note pitch bend and per note management have no MIDI 1.0 equivalent
            default -> new int[0];
        };
    }

    /** ex. {@code "40903C00 FFFF0000"} */
    public static String toString(int[] words) {
        StringBuilder sb = new StringBuilder();
        for (int word : words) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append("%08X".formatted(word));
        }
        return sb.toString();
    }
}
