/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.sf;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sun.media.sound.SoftSynthesizer;
import vavi.util.Debug;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/**
 * SfzSoundbankReaderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-09 nsano initial version <br>
 */
class SfzSoundbankReaderTest {

    @BeforeAll
    static void setup() throws Exception {
        Files.createDirectories(Path.of("tmp"));
    }

    /** generates a 440Hz sine wave WAV file and corresponding SFZ file */
    static void createTestSfz() throws Exception {
        float rate = 44100;
        int periods = 100;
        int frames = Math.round(periods * rate / 440);
        short[] sine = new short[frames];
        for (int i = 0; i < frames; i++) {
            sine[i] = (short) (Math.sin(2 * Math.PI * periods * i / frames) * 16384);
        }
        byte[] data = new byte[frames * 2];
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(sine);

        AudioFormat format = new AudioFormat(rate, 16, 1, true, false);
        AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(data), format, frames);
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File("tmp/test_sine.wav"));

        String sfzContent = """
                // Test SFZ
                <control>
                default_path=
                <global>
                ampeg_release=0.2
                <group>
                <region>
                sample=test_sine.wav
                key=69
                """;
        Files.writeString(Path.of("tmp/test.sfz"), sfzContent);
    }

    /** renders 3 seconds through gervill, returns the max amplitude, saves to wav */
    static int render(Soundbank soundbank, int note, String wav) throws Exception {
        try (SoftSynthesizer synthesizer = new SoftSynthesizer()) {
            AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
            AudioInputStream stream = synthesizer.openStream(format, null);
            assertTrue(synthesizer.loadAllInstruments(soundbank));

            Receiver receiver = synthesizer.getReceiver();
            receiver.send(new ShortMessage(ShortMessage.NOTE_ON, 0, note, 100), -1);

            byte[] pcm = new byte[(int) format.getFrameRate() * format.getFrameSize() * 3];
            new DataInputStream(stream).readFully(pcm);

            int max = 0;
            for (int i = 0; i < pcm.length; i += 2) {
                int v = Math.abs((short) ((pcm[i] & 0xff) | (pcm[i + 1] << 8)));
                if (v > max) max = v;
            }

            AudioInputStream out = new AudioInputStream(
                    new ByteArrayInputStream(pcm), format, pcm.length / format.getFrameSize());
            AudioSystem.write(out, AudioFileFormat.Type.WAVE, new File(wav));
            return max;
        }
    }

    @Test
    void test0() throws Exception {
        createTestSfz();

        Soundbank soundbank = MidiSystem.getSoundbank(new File("tmp/test.sfz"));
        assertNotNull(soundbank);
        assertEquals("test", soundbank.getName());
        assertEquals(1, soundbank.getInstruments().length);

        int max = render(soundbank, 69, "tmp/sfz-sine.wav");
        Debug.println("sfz max amplitude: " + max);
        assertTrue(max > 1000, "rendered audio is silent");
    }

    @Test
    void test1() throws Exception {
        // not an sfz file -> null
        assertNull(new SfzSoundbankReader().getSoundbank(new File("pom.xml")));
    }

    /** tests with a real sfz file under user documents if available */
    @Test
    void test2() throws Exception {
        // Look for standard sforzando folder
        Path sforzando = Path.of(System.getProperty("user.home"), "Documents", "sforzando");
        assumeTrue(Files.isDirectory(sforzando), "no sforzando folder found");

        // Try to find a simple organ sfz
        Path sfzFile = sforzando.resolve("user/NoBudgetBand/Organ/B3/b3_organ.sfz");
        assumeTrue(Files.exists(sfzFile), "no b3_organ.sfz found");

        Soundbank soundbank = MidiSystem.getSoundbank(sfzFile.toFile());
        assertNotNull(soundbank);
        Debug.println("Real SFZ soundbank loaded: " + soundbank.getName() + " with " + soundbank.getInstruments().length + " instruments");
    }
}
