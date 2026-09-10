/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;


/**
 * struct MIDIEventList of CoreMIDI, a list of Universal MIDI Packets.
 * <p>
 * Both {@code MIDIEventList} and {@code MIDIEventPacket} are variable length structs declared under
 * {@code #pragma pack(4)}, which JNA cannot express, so the memory is laid out by hand here. This
 * one holds a single packet, which is all {@link AudioToolbox#MusicDeviceMIDIEventList} needs.
 * <pre>
 *  0 SInt32 protocol             MIDIEventList
 *  4 UInt32 numPackets
 *  8 UInt64 packet[0].timeStamp  MIDIEventPacket
 * 16 UInt32 packet[0].wordCount
 * 20 UInt32 packet[0].words[]
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/09/11 nsano initial version <br>
 */
public class MIDIEventList {

    /** MIDIProtocolID */
    public static final int kMIDIProtocol_1_0 = 1;

    /** MIDIProtocolID */
    public static final int kMIDIProtocol_2_0 = 2;

    /** {@code MIDIEventPacket#words} is declared as 64 words long */
    public static final int MAX_WORDS = 64;

    private static final int OFFSET_PROTOCOL = 0;
    private static final int OFFSET_NUM_PACKETS = 4;
    private static final int OFFSET_TIME_STAMP = 8;
    private static final int OFFSET_WORD_COUNT = 16;
    private static final int OFFSET_WORDS = 20;

    /** kept as a field so that the native memory outlives the call it is passed to */
    private final Memory memory = new Memory(OFFSET_WORDS + (long) MAX_WORDS * Integer.BYTES);

    /** @param protocol MIDIProtocolID, {@link #kMIDIProtocol_1_0} or {@link #kMIDIProtocol_2_0} */
    public MIDIEventList(int protocol) {
        memory.clear();
        memory.setInt(OFFSET_PROTOCOL, protocol);
        memory.setInt(OFFSET_NUM_PACKETS, 1);
    }

    /** @return MIDIProtocolID */
    public int protocol() {
        return memory.getInt(OFFSET_PROTOCOL);
    }

    /**
     * Fills the single packet of this list.
     *
     * @param timeStamp MIDITimeStamp, 0 means "now"
     * @param words a stream of complete Universal MIDI Packets, native endian
     * @return the pointer to hand to native code, valid until the next call
     */
    public Pointer packet(long timeStamp, int... words) {
        if (words.length == 0 || words.length > MAX_WORDS) {
            throw new IllegalArgumentException("word count out of range: " + words.length);
        }
        memory.setLong(OFFSET_TIME_STAMP, timeStamp);
        memory.setInt(OFFSET_WORD_COUNT, words.length);
        memory.write(OFFSET_WORDS, words, 0, words.length);
        return memory;
    }
}
