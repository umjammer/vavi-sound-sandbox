/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.rococoa;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import vavi.sound.midi.ump.MidiClipFile;
import vavi.sound.midi.ump.UmpPlayer;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Plays MIDI Clip Files ({@code .midi2}) on AVFoundation.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/09/11 nsano initial version <br>
 */
@EnabledOnOs(OS.MAC)
@PropsEntity(url = "file:local.properties")
class RococoaUmpPlayerTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume.midi")
    float volume = 0.2f;

    /** {@code "manufacturer:subtype"} of the music device to play on */
    @Property(name = "midi2.audesc")
    String audesc = "appl:dls ";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    static Path midi2(String name) {
        return Path.of("src/test/resources/midi2", name + ".midi2");
    }

    /** plays the clip and gives back how long that took, in milliseconds */
    long play(String name) throws Exception {
        MidiClipFile clip = MidiClipFile.read(midi2(name));
Debug.println(name + ": " + clip.getEvents().size() + " events, " + clip.getTickLength() + " ticks");

        try (RococoaUmpReceiver receiver = new RococoaUmpReceiver(audesc)) {
            assertTrue(receiver.isOpen());
Debug.println("audio unit talks MIDI " + (receiver.isMidi2() ? "2.0" : "1.0"));
            receiver.setVolume(volume);

            long start = System.currentTimeMillis();
            new UmpPlayer(receiver).play(clip);
            long time = System.currentTimeMillis() - start;

            // every packet was taken by core audio, no OSStatus came back
            assertEquals(0, receiver.getErrorCount());
            return time;
        }
    }

    @Test
    @DisplayName("a C major scale of MIDI 1.0 messages in universal packets")
    void midi1() throws Exception {
        // 8 quarter notes at 120 bpm
        assertTimely(4000, play("test-c-major-scale-m1-g0"));
    }

    @Test
    @DisplayName("the same scale as MIDI 2.0 channel voice messages")
    void midi2() throws Exception {
        assertTimely(4000, play("test-c-major-scale-m2-g0"));
    }

    @Test
    @DisplayName("metadata, a GM2 System On SysEx and a bank select, then a dog barks three times")
    void doggy() throws Exception {
        assertTimely(1500, play("test-gm2-doggy-78-00-38-4c"));
    }

    @Test
    @DisplayName("a clip of nothing but its markers is over at once")
    void minimal() throws Exception {
        assertTimely(0, play("test-minimal"));
    }

    @Test
    @DisplayName("stopping cuts the clip short")
    void stop() throws Exception {
        MidiClipFile clip = MidiClipFile.read(midi2("test-c-major-scale-m1-g0"));

        try (RococoaUmpReceiver receiver = new RococoaUmpReceiver(audesc)) {
            receiver.setVolume(volume);
            UmpPlayer player = new UmpPlayer(receiver);
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
                player.stop();
            }).start();

            long start = System.currentTimeMillis();
            player.play(clip);
            long time = System.currentTimeMillis() - start;
Debug.println("stopped after " + time + " ms");

            assertFalse(player.isPlaying());
            // it gave up somewhere around the second note, well before the 4 seconds of the whole scale
            assertTrue(time < 3000, "played " + time + " ms");
        }
    }

    @Test
    @DisplayName("an audio unit that is not there is refused, not fatal")
    void noSuchAudioUnit() {
        assertThrows(IllegalArgumentException.class, () -> new RococoaUmpReceiver("vavi:nope"));
    }

    /** the player is a real time one, so the clip may not race through nor drag */
    static void assertTimely(long expected, long actual) {
Debug.println("played in " + actual + " ms, expected " + expected + " ms");
        assertTrue(actual >= expected * 0.9, "too fast: " + actual + " ms");
        assertTrue(actual <= expected + 1500, "too slow: " + actual + " ms");
    }
}
