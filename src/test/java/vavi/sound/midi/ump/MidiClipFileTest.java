/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.ump;

import java.nio.file.Path;
import java.util.List;
import javax.sound.midi.InvalidMidiDataException;

import vavi.sound.midi.ump.MidiClipFile.Event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * MidiClipFileTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/09/11 nsano initial version <br>
 */
class MidiClipFileTest {

    static Path midi2(String name) {
        return Path.of("src/test/resources/midi2", name + ".midi2");
    }

    @Test
    @DisplayName("a clip of nothing but its two markers")
    void minimal() throws Exception {
        MidiClipFile clip = MidiClipFile.read(midi2("test-minimal"));

        assertEquals(24, clip.getResolution());
        assertEquals(2, clip.getEvents().size());
        assertEquals(Ump.START_OF_CLIP, Ump.streamStatus(clip.getEvents().getFirst().words()[0]));
        assertEquals(Ump.END_OF_CLIP, Ump.streamStatus(clip.getEvents().getLast().words()[0]));
        assertEquals(0, clip.getTickLength());
    }

    @Test
    @DisplayName("delta clockstamps put the notes of a scale a quarter note apart")
    void midi1Scale() throws Exception {
        MidiClipFile clip = MidiClipFile.read(midi2("test-c-major-scale-m1-g0"));

        assertEquals(96, clip.getResolution());
        // set tempo, start of clip, 8 notes on and off, end of clip
        assertEquals(1 + 1 + 16 + 1, clip.getEvents().size());
        assertEquals(96 * 8, clip.getTickLength());

        // 0.5 s a quarter note at the 120 bpm the clip sets
        assertTrue(Ump.isSetTempo(clip.getEvents().getFirst().words()));
        assertEquals(50_000_000, clip.getEvents().getFirst().words()[1]);

        List<Event> notes = clip.getEvents().stream()
                .filter(e -> Ump.messageType(e.words()[0]) == Ump.MIDI1_CHANNEL_VOICE)
                .filter(e -> Ump.status(e.words()[0]) == Ump.NOTE_ON)
                .toList();
        assertEquals(8, notes.size());
        // C4 D4 E4 F4 G4 A4 B4 C5, one every 96 ticks
        assertArrayEquals(new int[] {60, 62, 64, 65, 67, 69, 71, 72},
                notes.stream().mapToInt(e -> (e.words()[0] >> 8) & 0x7f).toArray());
        assertArrayEquals(new long[] {0, 96, 192, 288, 384, 480, 576, 672},
                notes.stream().mapToLong(Event::tick).toArray());
    }

    @Test
    @DisplayName("the same scale in MIDI 2.0, at 16 bit velocity")
    void midi2Scale() throws Exception {
        MidiClipFile clip = MidiClipFile.read(midi2("test-c-major-scale-m2-g0"));

        List<Event> notes = clip.getEvents().stream()
                .filter(e -> Ump.messageType(e.words()[0]) == Ump.MIDI2_CHANNEL_VOICE)
                .filter(e -> Ump.status(e.words()[0]) == Ump.NOTE_ON)
                .toList();
        assertEquals(8, notes.size());
        assertArrayEquals(new int[] {60, 62, 64, 65, 67, 69, 71, 72},
                notes.stream().mapToInt(e -> (e.words()[0] >> 8) & 0x7f).toArray());
        // 0xffff, the loudest a MIDI 2.0 note on gets
        assertArrayEquals(new int[] {0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff},
                notes.stream().mapToInt(e -> e.words()[1] >>> 16).toArray());
    }

    @Test
    @DisplayName("the MIDI 2.0 scale down converts to exactly the MIDI 1.0 one, note for note")
    void midi2ScaleIsTheMidi1Scale() throws Exception {
        MidiClipFile midi1 = MidiClipFile.read(midi2("test-c-major-scale-m1-g0"));
        MidiClipFile midi2 = MidiClipFile.read(midi2("test-c-major-scale-m2-g0"));

        assertEquals(midi1.getResolution(), midi2.getResolution());
        assertArrayEquals(noteOns(midi1), noteOns(midi2));
        assertArrayEquals(noteOnTicks(midi1), noteOnTicks(midi2));
    }

    /** every note on of the clip, as the MIDI 1.0 message it is or becomes */
    static int[] noteOns(MidiClipFile clip) {
        return clip.getEvents().stream()
                .map(e -> Ump.toMidi1(e.words()))
                .filter(MidiClipFileTest::isNoteOn)
                .mapToInt(words -> words[0])
                .toArray();
    }

    static long[] noteOnTicks(MidiClipFile clip) {
        return clip.getEvents().stream()
                .filter(e -> isNoteOn(Ump.toMidi1(e.words())))
                .mapToLong(Event::tick)
                .toArray();
    }

    static boolean isNoteOn(int[] words) {
        return words.length == 1 &&
                Ump.messageType(words[0]) == Ump.MIDI1_CHANNEL_VOICE &&
                Ump.status(words[0]) == Ump.NOTE_ON;
    }

    @Test
    @DisplayName("metadata text, a SysEx and a program change with a bank")
    void doggy() throws Exception {
        MidiClipFile clip = MidiClipFile.read(midi2("test-gm2-doggy-78-00-38-4c"));

        // the MIDI Clip Name, a text that fits one message
        Event name = clip.getEvents().stream()
                .filter(e -> Ump.isText(e.words()) && Ump.flexStatus(e.words()[0]) == 0x03)
                .findFirst().orElseThrow();
        assertEquals("GM2 Doggy", Ump.text(name.words()));

        // GM2 System On, as a complete SysEx7 of 4 bytes
        Event sysex = clip.getEvents().stream()
                .filter(e -> Ump.messageType(e.words()[0]) == Ump.SYSEX7)
                .findFirst().orElseThrow();
        assertArrayEquals(new int[] {0x30047e7f, 0x09030000}, sysex.words());

        Event programChange = clip.getEvents().stream()
                .filter(e -> Ump.messageType(e.words()[0]) == Ump.MIDI2_CHANNEL_VOICE)
                .filter(e -> Ump.status(e.words()[0]) == Ump.PROGRAM_CHANGE)
                .findFirst().orElseThrow();
        // bank 0x78:0x00 program 0x38, the standard drum kit of GM2
        assertArrayEquals(new int[] {0x40c00001, 0x38007800}, programChange.words());
    }

    @Test
    @DisplayName("nothing but the header is a clip with no events")
    void empty() throws Exception {
        MidiClipFile clip = MidiClipFile.read(midi2("test-empty"));

        assertEquals(0, clip.getResolution());
        assertTrue(clip.getEvents().isEmpty());
    }

    @Test
    @DisplayName("a file without the SMF2CLIP header is refused")
    void notAClip() {
        assertThrows(InvalidMidiDataException.class, () -> MidiClipFile.read(midi2("test-not-a-midi-file")));
    }

    @Test
    @DisplayName("a MIDI 2.0 note on down converts to a MIDI 1.0 one, velocity and all")
    void toMidi1Note() {
        // note on, group 0, channel 0, C4, velocity 0xffff
        assertArrayEquals(new int[] {0x20903c7f}, Ump.toMidi1(0x40903c00, 0xffff0000));
        // a MIDI 2.0 note on of velocity 0 is still a note on, so it may not become a velocity of 0
        assertArrayEquals(new int[] {0x20903c01}, Ump.toMidi1(0x40903c00, 0x00000000));
        // note off keeps its velocity as it is
        assertArrayEquals(new int[] {0x20803c40}, Ump.toMidi1(0x40803c00, 0x80000000));
    }

    @Test
    @DisplayName("a MIDI 2.0 program change with a bank down converts to two bank selects and a program change")
    void toMidi1ProgramChange() {
        assertArrayEquals(new int[] {0x20b00078, 0x20b02000, 0x20c03800}, Ump.toMidi1(0x40c00001, 0x38007800));
        // without the bank valid flag there is no bank select to send
        assertArrayEquals(new int[] {0x20c03800}, Ump.toMidi1(0x40c00000, 0x38007800));
    }

    @Test
    @DisplayName("the wider MIDI 2.0 controllers lose their resolution but keep their value")
    void toMidi1Controllers() {
        // control change 7 of the loudest 32 bit value
        assertArrayEquals(new int[] {0x20b0077f}, Ump.toMidi1(0x40b00700, 0xffffffff));
        // pitch bend of dead centre, that is 0x2000 over a data byte pair
        assertArrayEquals(new int[] {0x20e00040}, Ump.toMidi1(0x40e00000, 0x80000000));
        // a per note controller has nowhere to go in MIDI 1.0
        assertArrayEquals(new int[0], Ump.toMidi1(0x40003c01, 0xffffffff));
        // anything that is not MIDI 2.0 Channel Voice passes through
        assertArrayEquals(new int[] {0x20903c7f}, Ump.toMidi1(0x20903c7f));
    }
}
