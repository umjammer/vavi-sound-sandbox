/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.exs;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sun.media.sound.SoftSynthesizer;
import vavi.util.Debug;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/**
 * ExsSoundbankReaderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-04 nsano initial version <br>
 */
class ExsSoundbankReaderTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "/exs/MC-202 bass.exs",
            "/exs/Big News (slow sweeps).exs",
            "/exs/K3 Big.exs",
            "/exs/filter-DFAM-WFM-LP.exs",
            "/exs/Shape-DFAM-PSEQOUT.exs",
            "/exs/Hi Hat 909 Clean.exs"
    })
    void test0(String filename) throws Exception {
        File file = Path.of(ExsSoundbankReaderTest.class.getResource(filename).toURI()).toFile();
        Soundbank soundbank = MidiSystem.getSoundbank(file);
        assertNotNull(soundbank);
        assertEquals(file.getName(), soundbank.getName());
        assertEquals(1, soundbank.getInstruments().length);
    }

    @Test
    void test1() throws Exception {
        // not an exs file -> null, so other providers get a chance
        assertNull(new ExsSoundbankReader().getSoundbank(new File("pom.xml")));
    }

    static final String gbExs =
            "/Library/Application Support/GarageBand/Instrument Library/Sampler/Sampler Instruments/Church Organ/Full Organ.exs";

    /** render a note through gervill and check it's not silence */
    @Test
    void test2() throws Exception {
        assumeTrue(Files.exists(Path.of(gbExs)), "GarageBand is not installed");

        Soundbank soundbank = MidiSystem.getSoundbank(new File(gbExs));
        assertNotNull(soundbank);
        assertEquals(1, soundbank.getInstruments().length);

        try (SoftSynthesizer synthesizer = new SoftSynthesizer()) {
            AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
            AudioInputStream stream = synthesizer.openStream(format, null);
            assertTrue(synthesizer.loadAllInstruments(soundbank));

            Receiver receiver = synthesizer.getReceiver();
            receiver.send(new ShortMessage(ShortMessage.NOTE_ON, 0, 48, 100), -1);

            byte[] pcm = new byte[(int) format.getFrameRate() * format.getFrameSize() * 3];
            new DataInputStream(stream).readFully(pcm);

            int max = 0;
            for (int i = 0; i < pcm.length; i += 2) {
                int v = Math.abs((short) ((pcm[i] & 0xff) | (pcm[i + 1] << 8)));
                if (v > max) max = v;
            }
Debug.println("max amplitude: " + max);
            assertTrue(max > 100, "rendered audio is silent");

            Files.createDirectories(Path.of("tmp"));
            AudioInputStream out = new AudioInputStream(
                    new ByteArrayInputStream(pcm), format, pcm.length / format.getFrameSize());
            AudioSystem.write(out, AudioFileFormat.Type.WAVE, new File("tmp/exs.wav"));
        }
    }
}
