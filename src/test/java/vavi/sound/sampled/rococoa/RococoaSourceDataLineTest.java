/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.rococoa;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import com.sun.media.sound.SoftSynthesizer;

import vavi.sound.SoundUtil;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;
import vavix.rococoa.avfoundation.AVAudioUnitEffect;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vavi.sound.midi.MidiUtil.volume;


/**
 * RococoaSourceDataLineTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-08 nsano initial version <br>
 */
@EnabledOnOs(OS.MAC)
@PropsEntity(url = "file:local.properties")
class RococoaSourceDataLineTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    static boolean onIde = System.getProperty("vavi.test", "").equals("ide");

    /** AUMatrixReverb, then AUDelay */
    static final String effects = "appl:mrev,appl:dely";

    @Property(name = "vavi.test.volume")
    float volume = 0.2f;

    @Property(name = "vavi.test.volume.midi")
    float midiVolume = 0.2f;

    @Property
    String midi = "src/test/resources/test.mid";

    @Property
    String wav = "src/test/resources/test.wav";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    /** 440 Hz, 16 bit signed little endian stereo */
    static byte[] tone(AudioFormat format, double seconds) {
        int frames = (int) (format.getSampleRate() * seconds);
        byte[] pcm = new byte[frames * format.getFrameSize()];
        for (int f = 0; f < frames; f++) {
            short v = (short) (Math.sin(2 * Math.PI * 440 * f / format.getSampleRate()) * Short.MAX_VALUE * 0.3);
            for (int c = 0; c < format.getChannels(); c++) {
                int p = (f * format.getChannels() + c) * 2;
                pcm[p] = (byte) v;
                pcm[p + 1] = (byte) (v >> 8);
            }
        }
        return pcm;
    }

    @Test
    @DisplayName("pcm through the AudioUnit effect chain")
    void test1() throws Exception {
        AudioFormat format = new AudioFormat(44100, 16, 2, true, false);

        RococoaSourceDataLine line = new RococoaSourceDataLine();
        line.setEffects(effects);
        line.open(format);
        assertEquals(2, line.getEffects().size());
        for (AVAudioUnitEffect effect : line.getEffects()) {
Debug.println("effect: " + effect.name() + " by " + effect.manufacturerName());
            assertNotNull(effect.name());
        }
        SoundUtil.volume(line, volume);
        line.start();

        byte[] pcm = tone(format, 2);
        int written = line.write(pcm, 0, pcm.length);
        line.drain();
        line.stop();

        assertEquals(pcm.length, written);
        // core audio really consumed the data, at the rate it should have
        long frames = line.getLongFramePosition();
Debug.println("frames: " + frames + " / " + pcm.length / format.getFrameSize());
        assertTrue(frames >= ((double) pcm.length / format.getFrameSize()) * 0.9, "played " + frames + " frames");

        line.close();
        assertFalse(line.isOpen());
    }

    @Test
    @DisplayName("the line is reachable through the mixer spi")
    void test2() throws Exception {
        AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
        javax.sound.sampled.Mixer mixer = AudioSystem.getMixer(RococoaMixer.mixerInfo);
        assertTrue(mixer.isLineSupported(new javax.sound.sampled.DataLine.Info(SourceDataLine.class, format)));
        SourceDataLine line = (SourceDataLine) mixer.getLine(new javax.sound.sampled.DataLine.Info(SourceDataLine.class, format));
        assertInstanceOf(RococoaSourceDataLine.class, line);

        // ... but we must not become the default output, the jdk providers still come first
        try (SourceDataLine dflt = AudioSystem.getSourceDataLine(format)) {
            assertFalse(dflt instanceof RococoaSourceDataLine, "" + dflt);
        }
    }

    /**
     * gervill rendering into the AudioUnit chain, the line handed over directly.
     * this is the way when you want to keep a handle on the effects.
     */
    @Test
    @DisplayName("gervill -> AudioUnit effects (line handed over)")
    void test3() throws Exception {
        RococoaSourceDataLine line = new RococoaSourceDataLine();
        line.setEffects(effects);
        // gervill takes its format from the line, so the line goes first
        line.open(new AudioFormat(44100, 16, 2, true, false));

        try (SoftSynthesizer synthesizer = new SoftSynthesizer()) {
            synthesizer.open(line, null);
Debug.println("line: " + line.getFormat() + ", effects: " + line.getEffects().size());
            assertTrue(line.isOpen());

            play(synthesizer);
        }
    }

    /**
     * the same, but wired by the spi only, which is what an unmodified player does.
     * gervill asks {@code AudioSystem} for the line, {@code javax.sound.sampled.SourceDataLine}
     * decides that it is ours.
     */
    @Test
    @DisplayName("gervill -> AudioUnit effects (by system property)")
    void test4() throws Exception {
        System.setProperty("javax.sound.midi.Synthesizer", "#Gervill");
        System.setProperty("javax.sound.sampled.SourceDataLine", "#Rococoa Mixer");
        System.setProperty(RococoaSourceDataLine.class.getName() + ".effects", effects);
        try {
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
Debug.println("synthesizer: " + synthesizer);

            play(synthesizer);

            synthesizer.close();
        } finally {
            System.setProperty("javax.sound.sampled.SourceDataLine", "");
            System.setProperty("javax.sound.midi.Synthesizer", "");
        }
    }

    /** plays {@link #midi} through the given synthesizer */
    void play(Synthesizer synthesizer) throws Exception {
        Sequencer sequencer = MidiSystem.getSequencer(false);
        Receiver receiver = synthesizer.getReceiver();
        sequencer.getTransmitter().setReceiver(receiver);
        sequencer.open();

        Sequence sequence = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(Path.of(midi))));
        sequencer.setSequence(sequence);

        CountDownLatch cdl = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
            if (meta.getType() == 47) cdl.countDown();
        };
        sequencer.addMetaEventListener(mel);
        sequencer.start();
        volume(receiver, midiVolume);

        if (onIde) {
            cdl.await();
        } else {
            Thread.sleep(5000);
            sequencer.stop();
        }

        sequencer.removeMetaEventListener(mel);
        sequencer.close();
    }

    /** wav through the chain, so you can hear what the effects do to real material */
    @Test
    @DisplayName("wav -> AudioUnit effects")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test5() throws Exception {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(Path.of(wav))))) {
            AudioFormat format = ais.getFormat();
Debug.println("format: " + format);

            RococoaSourceDataLine line = new RococoaSourceDataLine();
            line.setEffects(effects);
            line.open(format);
            SoundUtil.volume(line, volume);
            line.start();

            byte[] buffer = new byte[8192];
            int r;
            while ((r = ais.read(buffer, 0, buffer.length)) > 0) {
                line.write(buffer, 0, r);
            }
            line.drain();
            line.close();
        }
    }
}
