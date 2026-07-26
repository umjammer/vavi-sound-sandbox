/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.misc;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.getLogger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * AutoWiredFormatConversionProviderTest.
 * <p>
 * The conversions used here are the ones {@code javazoom.spi.mpeg.sampled.file.MonoTest} of
 * the {@code mp3spi} project has to wire by hand: {@code test.mp3} is a stereo mp3,
 * {@code mp3spi} decodes it but keeps the channel count, so anything monaural needs a second
 * provider behind it.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260726 nsano initial version <br>
 */
class AutoWiredFormatConversionProviderTest {

    private static final System.Logger logger = getLogger(AutoWiredFormatConversionProviderTest.class.getName());

    static final Path mp3 = Path.of("src/test/resources/test.mp3");

    static final Path wav = Path.of("src/test/resources/mono.wav");

    /** stereo mp3 as read by mp3spi */
    static AudioInputStream mp3Stream() throws Exception {
        return AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(mp3)));
    }

    static AudioFormat pcm(float sampleRate, int channels) {
        return new AudioFormat(Encoding.PCM_SIGNED, sampleRate, 16, channels, channels * 2, sampleRate, false);
    }

    /** the provider is off by default here, so that each test says when it wants it */
    @BeforeEach
    void setUp() {
        System.setProperty("vavi.sound.sampled.spi.autowired", "false");
    }

    @AfterEach
    void tearDown() {
        System.setProperty("vavi.sound.sampled.spi.autowired", "false");
        System.clearProperty("vavi.sound.sampled.spi.autowired.max");
    }

    static void enable() {
        System.setProperty("vavi.sound.sampled.spi.autowired", "true");
    }

    @Test
    @DisplayName("registered as a spi")
    void test0() {
        assertTrue(ServiceLoader.load(FormatConversionProvider.class).stream()
                .anyMatch(p -> p.type() == AutoWiredFormatConversionProvider.class));
    }

    @Test
    @DisplayName("mp3 stereo -> pcm mono, which no single provider does")
    void test1() throws Exception {
        AudioFormat sourceFormat = mp3Stream().getFormat();
        AudioFormat targetFormat = pcm(sourceFormat.getSampleRate(), 1);
logger.log(DEBUG, "source: " + sourceFormat);
logger.log(DEBUG, "target: " + targetFormat);

        // without us this is the IllegalArgumentException of MonoTest#test4
        assertFalse(AudioSystem.isConversionSupported(targetFormat, sourceFormat));
        assertThrows(IllegalArgumentException.class, () -> AudioSystem.getAudioInputStream(targetFormat, mp3Stream()));

        enable();

        assertTrue(AudioSystem.isConversionSupported(targetFormat, sourceFormat));

        List<AudioFormat> chain = AutoWiredFormatConversionProvider.wire(sourceFormat, targetFormat);
        assertNotNull(chain);
logger.log(DEBUG, "chain: " + chain);
        assertEquals(2, chain.size()); // decode, then downmix
        assertEquals(2, chain.get(0).getChannels());
        assertEquals(targetFormat, chain.get(chain.size() - 1));

        try (AudioInputStream out = AudioSystem.getAudioInputStream(targetFormat, mp3Stream())) {
            assertEquals(Encoding.PCM_SIGNED, out.getFormat().getEncoding());
            assertEquals(1, out.getFormat().getChannels());
            assertEquals(sourceFormat.getSampleRate(), out.getFormat().getSampleRate());
            assertTrue(out.readNBytes(0x10000).length > 0);
        }
    }

    @Test
    @DisplayName("mp3 stereo -> ulaw mono 22050, a chain of three")
    void test2() throws Exception {
        AudioFormat sourceFormat = mp3Stream().getFormat();
        AudioFormat targetFormat = new AudioFormat(Encoding.ULAW, 22050, 8, 1, 1, 22050, false);

        assertFalse(AudioSystem.isConversionSupported(targetFormat, sourceFormat));

        enable();

        List<AudioFormat> chain = AutoWiredFormatConversionProvider.wire(sourceFormat, targetFormat);
        assertNotNull(chain);
logger.log(DEBUG, "chain: " + chain);
        assertEquals(3, chain.size()); // decode, then resample + downmix, then encode

        try (AudioInputStream out = AudioSystem.getAudioInputStream(targetFormat, mp3Stream())) {
            assertEquals(Encoding.ULAW, out.getFormat().getEncoding());
            assertEquals(1, out.getFormat().getChannels());
            assertEquals(22050f, out.getFormat().getSampleRate());
            assertTrue(out.readNBytes(0x10000).length > 0);
        }
    }

    @Test
    @DisplayName("a chain longer than the limit is not wired")
    void test3() throws Exception {
        AudioFormat sourceFormat = mp3Stream().getFormat();
        AudioFormat targetFormat = new AudioFormat(Encoding.ULAW, 22050, 8, 1, 1, 22050, false);

        enable();
        System.setProperty("vavi.sound.sampled.spi.autowired.max", "2");

        assertNull(AutoWiredFormatConversionProvider.wire(sourceFormat, targetFormat));
        assertFalse(AudioSystem.isConversionSupported(targetFormat, sourceFormat));

        // the chain of two of test1 is still found
        assertNotNull(AutoWiredFormatConversionProvider.wire(sourceFormat, pcm(sourceFormat.getSampleRate(), 1)));
    }

    @Test
    @DisplayName("target encoding only")
    void test4() throws Exception {
        AudioFormat sourceFormat = mp3Stream().getFormat();

        // decoding is what mp3spi advertises, we must not step in
        assertTrue(AudioSystem.isConversionSupported(Encoding.PCM_SIGNED, sourceFormat));
        // ... but nobody encodes ulaw straight out of mp3
        assertFalse(AudioSystem.isConversionSupported(Encoding.ULAW, sourceFormat));
        assertThrows(IllegalArgumentException.class, () -> AudioSystem.getAudioInputStream(Encoding.ULAW, mp3Stream()));

        enable();

        AutoWiredFormatConversionProvider provider = new AutoWiredFormatConversionProvider();
        assertFalse(provider.isConversionSupported(Encoding.PCM_SIGNED, sourceFormat));

        assertTrue(AudioSystem.isConversionSupported(Encoding.ULAW, sourceFormat));
        try (AudioInputStream out = AudioSystem.getAudioInputStream(Encoding.ULAW, mp3Stream())) {
            assertEquals(Encoding.ULAW, out.getFormat().getEncoding());
            assertTrue(out.readNBytes(0x10000).length > 0);
        }
    }

    @Test
    @DisplayName("stays out of the way when a single provider can do it")
    void test5() throws Exception {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(wav)))) {
            AudioFormat sourceFormat = in.getFormat();
            AudioFormat targetFormat = pcm(sourceFormat.getSampleRate(), 2);

            // mono -> stereo pcm is done by a single provider
            assertTrue(AudioSystem.isConversionSupported(targetFormat, sourceFormat));

            enable();

            AutoWiredFormatConversionProvider provider = new AutoWiredFormatConversionProvider();
            assertFalse(provider.isConversionSupported(targetFormat, sourceFormat));
            // and does not advertise it either
            assertTrue(List.of(provider.getTargetFormats(Encoding.PCM_SIGNED, sourceFormat)).stream()
                    .noneMatch(f -> f.matches(targetFormat)));
        }
    }

    @Test
    @DisplayName("hopeless conversion terminates instead of recursing")
    void test6() throws Exception {
        enable();

        AudioFormat sourceFormat = mp3Stream().getFormat();
        AudioFormat targetFormat = new AudioFormat(new Encoding("NOWHERE"), 44100, 16, 1, 2, 44100, false);

        assertNull(AutoWiredFormatConversionProvider.wire(sourceFormat, targetFormat));
        assertFalse(AudioSystem.isConversionSupported(targetFormat, sourceFormat));
        assertThrows(IllegalArgumentException.class, () -> AudioSystem.getAudioInputStream(targetFormat, mp3Stream()));
    }

    @Test
    @DisplayName("the whole point: a chained stream reads to the end")
    void test7() throws Exception {
        enable();

        AudioFormat sourceFormat = mp3Stream().getFormat();
        AudioFormat targetFormat = pcm(sourceFormat.getSampleRate(), 1);

        try (AudioInputStream out = AudioSystem.getAudioInputStream(targetFormat, mp3Stream())) {
            byte[] buf = new byte[8192];
            long total = 0;
            for (int r; (r = out.read(buf)) != -1; ) {
                total += r;
            }
logger.log(DEBUG, "read: " + total + " bytes");
            assertTrue(total > 0);
            assertEquals(0, total % out.getFormat().getFrameSize());
        }
    }

    @Test
    @DisplayName("advertises nothing while disabled")
    void test8() throws Exception {
        AudioFormat sourceFormat = mp3Stream().getFormat();
        AutoWiredFormatConversionProvider provider = new AutoWiredFormatConversionProvider();

        assertEquals(0, provider.getSourceEncodings().length);
        assertEquals(0, provider.getTargetEncodings().length);
        assertEquals(0, provider.getTargetEncodings(sourceFormat).length);
        assertEquals(0, provider.getTargetFormats(Encoding.PCM_SIGNED, sourceFormat).length);

        enable();

        assertTrue(provider.getSourceEncodings().length > 0);
        assertTrue(provider.getTargetEncodings().length > 0);
        assertTrue(provider.getTargetFormats(Encoding.PCM_SIGNED, sourceFormat).length > 0);
    }
}
