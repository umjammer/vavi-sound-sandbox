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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sun.media.sound.SF2Instrument;
import com.sun.media.sound.SF2InstrumentRegion;
import com.sun.media.sound.SF2Layer;
import com.sun.media.sound.SF2LayerRegion;
import com.sun.media.sound.SF2Region;
import com.sun.media.sound.SF2Sample;
import com.sun.media.sound.SF2Soundbank;
import com.sun.media.sound.SoftSynthesizer;
import vavi.sound.sf.SFont;
import vavi.util.Debug;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/**
 * SfSoundbankReaderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-04 nsano initial version <br>
 */
class SfSoundbankReaderTest {

    @BeforeAll
    static void setup() throws Exception {
        Files.createDirectories(Path.of("tmp"));
    }

    /** builds a minimal one instrument sf2: a looped 440Hz sine at root key 69 */
    static SF2Soundbank createTestSf2() {
        SF2Soundbank sf2 = new SF2Soundbank();
        sf2.setName("test");

        float rate = 44100;
        int periods = 100;
        int frames = Math.round(periods * rate / 440);
        short[] sine = new short[frames];
        for (int i = 0; i < frames; i++) {
            sine[i] = (short) (Math.sin(2 * Math.PI * periods * i / frames) * 16384);
        }
        byte[] data = new byte[frames * 2];
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(sine);

        SF2Sample sample = new SF2Sample(sf2);
        sample.setName("sine");
        sample.setData(data);
        sample.setSampleRate((long) rate);
        sample.setOriginalPitch(69);
        sample.setStartLoop(0);
        sample.setEndLoop(frames);
        sf2.addResource(sample);

        SF2Layer layer = new SF2Layer(sf2);
        layer.setName("sine");
        sf2.addResource(layer);
        SF2LayerRegion region = new SF2LayerRegion();
        region.setSample(sample);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        layer.getRegions().add(region);

        SF2Instrument instrument = new SF2Instrument(sf2);
        instrument.setName("sine");
        instrument.setPatch(new javax.sound.midi.Patch(0, 0));
        SF2InstrumentRegion instrumentRegion = new SF2InstrumentRegion();
        instrumentRegion.setLayer(layer);
        instrument.getRegions().add(instrumentRegion);
        sf2.addInstrument(instrument);

        return sf2;
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

    static File resource(String name) throws Exception {
        return Path.of(SfSoundbankReaderTest.class.getResource(name).toURI()).toFile();
    }

    /**
     * sf3 (vorbis) fixture through the spi and gervill.
     * the fixture wraps a sample extracted from MuseScore_General.sf3 (MIT),
     * the available pure java vorbis decoder rejects sox/libvorbis 1.3 streams.
     */
    @Test
    void test0() throws Exception {
        Soundbank soundbank = MidiSystem.getSoundbank(resource("/sf/test.sf3"));
        assertNotNull(soundbank);
        assertEquals("test", soundbank.getName());
        assertEquals(1, soundbank.getInstruments().length);

        int max = render(soundbank, 60, "tmp/sf3.wav");
Debug.println("sf3 max amplitude: " + max);
        assertTrue(max > 1000, "rendered audio is silent");
    }

    /** sf4 (flac) fixture through the spi and gervill */
    @Test
    void test0f() throws Exception {
        Soundbank soundbank = MidiSystem.getSoundbank(resource("/sf/test.sf4"));
        assertNotNull(soundbank);
        assertEquals("test", soundbank.getName());
        assertEquals(1, soundbank.getInstruments().length);

        int max = render(soundbank, 69, "tmp/sf4.wav");
Debug.println("sf4 max amplitude: " + max);
        assertTrue(max > 1000, "rendered audio is silent");
    }

    @Test
    void test1() throws Exception {
        // not a soundfont -> null, so other providers get a chance
        assertNull(new SfSoundbankReader().getSoundbank(new File("pom.xml")));

        // plain sf2 is not ours either, the jdk reads those
        File sf2File = new File("tmp/test.sf2");
        createTestSf2().save(sf2File);
        assertNull(new SfSoundbankReader().getSoundbank(sf2File));
    }

    static final String[] sf3Files = {
            System.getProperty("user.home") + "/Library/Audio/Sounds/Banks/Orchestra/MuseScore_General.sf3",
            "/Applications/MuseScore 4.app/Contents/Resources/sound/MS Basic.sf3"
    };

    static File findMuseScoreSf3() {
        for (String path : sf3Files) {
            if (Files.exists(Path.of(path))) {
                return new File(path);
            }
        }
        return null;
    }

    /** a real MuseScore soundfont through the spi and gervill */
    @Test
    void test2() throws Exception {
        File file = findMuseScoreSf3();
        assumeTrue(file != null, "no MuseScore sf3 found");

        Soundbank soundbank = MidiSystem.getSoundbank(file);
        assertNotNull(soundbank);
Debug.println("soundbank: " + soundbank.getName() + ", instruments: " + soundbank.getInstruments().length);
        assertTrue(soundbank.getInstruments().length > 100);

        int max = render(soundbank, 60, "tmp/sf3-piano.wav");
Debug.println("sf3 max amplitude: " + max);
        assertTrue(max > 1000, "rendered audio is silent");
    }

    /** expand sf3 -> sf2 with the SFont writer, then verify the jdk can read the result */
    @Test
    void test3() throws Exception {
        File file = findMuseScoreSf3();
        assumeTrue(file != null, "no MuseScore sf3 found");

        SFont.SoundFont sf = new SFont.SoundFont(file);
        sf.read();
        int presets = sf.getPresets().size();
        File sf2File = new File("tmp/expanded.sf2");
        sf.write(sf2File, SFont.FileType.SF2Format, 2);

        SF2Soundbank sf2 = new SF2Soundbank(sf2File);
        assertEquals(presets, sf2.getInstruments().length);

        int max = render(sf2, 60, "tmp/sf2-piano.wav");
Debug.println("expanded sf2 max amplitude: " + max);
        assertTrue(max > 1000, "rendered audio is silent");
    }
}
