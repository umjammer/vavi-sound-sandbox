/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.ump;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.sound.midi.InvalidMidiDataException;


/**
 * A MIDI Clip File, the {@code .midi2} counterpart of a Standard MIDI File.
 * <p>
 * The file is the 8 byte magic {@value #MAGIC} followed by nothing but Universal MIDI Packets. Time
 * is not stored per message as in an SMF, it is a Utility message of its own, the Delta Clockstamp,
 * that pushes the clock forward for everything that follows it. How long a tick is comes from the
 * Delta Clockstamp Ticks Per Quarter Note message together with the Flex Data Set Tempo, and when
 * the clip has no DCTPQ, a tick is {@code 1/}{@value #DEFAULT_TICKS_PER_SECOND} second instead.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/09/11 nsano initial version <br>
 * @see "https://midi.org/midi-clip-file-specification-smf2clip"
 */
public class MidiClipFile {

    /** the file header */
    public static final String MAGIC = "SMF2CLIP";

    /** a Delta Clockstamp tick is 1/31250 second when the clip has no {@link Ump#DCTPQ} */
    public static final int DEFAULT_TICKS_PER_SECOND = 31250;

    /**
     * One Universal MIDI Packet message at the point in time the Delta Clockstamps put it.
     *
     * @param tick the clip clock, in {@link #getResolution()} units
     * @param words one complete UMP message, 1 to 4 words
     */
    public record Event(long tick, int[] words) {

        @Override
        public String toString() {
            return tick + ": " + Ump.toString(words);
        }
    }

    private final int resolution;

    private final List<Event> events;

    private MidiClipFile(int resolution, List<Event> events) {
        this.resolution = resolution;
        this.events = events;
    }

    /**
     * Delta Clockstamp ticks per quarter note, or 0 when the clip carries no {@link Ump#DCTPQ}, in
     * which case a tick is a fixed {@code 1/}{@value #DEFAULT_TICKS_PER_SECOND} second.
     */
    public int getResolution() {
        return resolution;
    }

    /** every message of the clip in file order, timing messages excluded, they are in the ticks */
    public List<Event> getEvents() {
        return events;
    }

    /** the length of the clip in ticks */
    public long getTickLength() {
        return events.isEmpty() ? 0 : events.getLast().tick();
    }

    public static MidiClipFile read(Path path) throws IOException, InvalidMidiDataException {
        try (InputStream in = Files.newInputStream(path)) {
            return read(in);
        }
    }

    /**
     * @throws InvalidMidiDataException not a MIDI Clip File, or one that ends mid message
     */
    public static MidiClipFile read(InputStream in) throws IOException, InvalidMidiDataException {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(in));

        byte[] magic = new byte[MAGIC.length()];
        try {
            dis.readFully(magic);
        } catch (EOFException e) {
            throw new InvalidMidiDataException("too short for a MIDI Clip File");
        }
        if (!Arrays.equals(magic, MAGIC.getBytes(StandardCharsets.US_ASCII))) {
            throw new InvalidMidiDataException("not a MIDI Clip File: " + new String(magic, StandardCharsets.US_ASCII));
        }

        int resolution = 0;
        long tick = 0;
        List<Event> events = new ArrayList<>();
        while (true) {
            int word;
            try {
                word = dis.readInt();
            } catch (EOFException e) {
                break;
            }
            int[] words = new int[Ump.words(word)];
            words[0] = word;
            try {
                for (int i = 1; i < words.length; i++) {
                    words[i] = dis.readInt();
                }
            } catch (EOFException e) {
                throw new InvalidMidiDataException("clip ends in the middle of a message: " + Ump.toString(words));
            }
            if (Ump.messageType(word) == Ump.UTILITY) {
                // timing is not an event, it is what puts the events in time
                switch (Ump.status(word)) {
                    case Ump.DCTPQ -> resolution = word & 0xffff;
                    case Ump.DCS -> tick += Ump.data(word);
                    default -> { // NOOP, and jitter reduction which a file has no use for
                    }
                }
                continue;
            }
            events.add(new Event(tick, words));
        }
        return new MidiClipFile(resolution, Collections.unmodifiableList(events));
    }
}
