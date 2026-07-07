/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.xma;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vavi.sound.SoundUtil.volume;
import static vavi.sound.sampled.xma.XmaEncoding.XMA;
import static vavix.util.DelayedWorker.later;


/**
 * WmaFormatConversionProviderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260707 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class XmaFormatConversionProviderTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    static boolean onIde = System.getProperty("vavi.test", "").equals("ide");
    static long time = onIde ? 1000 * 1000 : 10 * 1000;

    /** an .xma test file; supply your own via local.properties (key "xma") */
    @Property(name = "xma")
    String inFile = "src/test/resources/test.xma";

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    boolean inFileExists() {
        return Files.exists(Paths.get(inFile));
    }

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @Test
    @DisplayName("directly")
    @EnabledIf("inFileExists")
    public void test0() throws Exception {
        Path path = Paths.get(inFile);
        AudioInputStream sourceAis = new XmaAudioFileReader().getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)));

        AudioFormat inAudioFormat = sourceAis.getFormat();
Debug.println("IN: " + inAudioFormat);
        AudioFormat outAudioFormat = new AudioFormat(
                inAudioFormat.getSampleRate(),
                16,
                inAudioFormat.getChannels(),
                true,
                false);
Debug.println("OUT: " + outAudioFormat);

        assertTrue(AudioSystem.isConversionSupported(outAudioFormat, inAudioFormat));

        AudioInputStream secondAis = new XmaFormatConversionProvider().getAudioInputStream(outAudioFormat, sourceAis);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, secondAis.getFormat());
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(secondAis.getFormat());
        line.start();

        volume(line, volume);

        byte[] buf = new byte[1024];
        while (!later(time).come()) {
            int r = secondAis.read(buf, 0, 1024);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }
        line.drain();
        line.stop();
        line.close();
    }

    @Test
    @DisplayName("by spi")
    @EnabledIf("inFileExists")
    public void test1() throws Exception {
        Path path = Paths.get(inFile);
        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)));

        AudioFormat inAudioFormat = sourceAis.getFormat();
Debug.println("IN: " + inAudioFormat);
        AudioFormat outAudioFormat = new AudioFormat(
                inAudioFormat.getSampleRate(),
                16,
                inAudioFormat.getChannels(),
                true,
                false);
Debug.println("OUT: " + outAudioFormat);

        assertTrue(AudioSystem.isConversionSupported(outAudioFormat, inAudioFormat));

        AudioInputStream secondAis = AudioSystem.getAudioInputStream(outAudioFormat, sourceAis);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, secondAis.getFormat());
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(secondAis.getFormat());
        line.start();

        volume(line, volume);

        byte[] buf = new byte[1024];
        while (!later(time).come()) {
            int r = secondAis.read(buf, 0, 1024);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }
        line.drain();
        line.stop();
        line.close();
    }

    @Test
    @DisplayName("recognises encoding")
    @EnabledIf("inFileExists")
    void test2() throws Exception {
        AudioInputStream ais = AudioSystem.getAudioInputStream(Paths.get(inFile).toFile());
        assertEquals(XMA, ais.getFormat().getEncoding());
    }

    @Test
    @DisplayName("an ASF .xma (not XMA) is unsupported and the stream is untouched")
    void test5() throws Exception {
        InputStream is = new BufferedInputStream(XmaFormatConversionProviderTest.class.getResourceAsStream("/test.wma"));
        int available = is.available();
        UnsupportedAudioFileException e = assertThrows(UnsupportedAudioFileException.class, () -> {
            new XmaAudioFileReader().getAudioInputStream(is);
        });
Debug.println(e.getMessage());
        assertEquals(available, is.available()); // spi must not consume input stream even one byte
    }

    /**
     * Full pipeline on a synthetic, well-formed XMA2 RIFF (one all-zero packet):
     * header probe, container parse, frame de-packetising, decode loop and PCM
     * conversion must all run and terminate cleanly at EOF.
     */
    @Test
    @DisplayName("end-to-end plumbing on a synthetic XMA2 file")
    void test6() throws Exception {
        byte[] xma = buildMinimalXma2(48000, 2);

        AudioInputStream sourceAis = new XmaAudioFileReader().getAudioInputStream(new ByteArrayInputStream(xma));
        AudioFormat in = sourceAis.getFormat();
        assertEquals(XMA, in.getEncoding());
        assertEquals(48000f, in.getSampleRate());
        assertEquals(2, in.getChannels());

        AudioFormat out = new AudioFormat(in.getSampleRate(), 16, in.getChannels(), true, false);
        assertTrue(AudioSystem.isConversionSupported(out, in));

        AudioInputStream pcm = new XmaFormatConversionProvider().getAudioInputStream(out, sourceAis);
        int total = pcm.readAllBytes().length; // an empty (silent) stream reaches EOF without throwing
        Debug.println("decoded pcm bytes: " + total);
        pcm.close();
    }

    /** Minimal RIFF/WAVE XMA2 container: WAVEFORMATEX fmt + one zero-filled 2048-byte packet. */
    static byte[] buildMinimalXma2(int sampleRate, int channels) {
        // fmt : 18-byte WAVEFORMATEX
        ByteBuffer fmt = ByteBuffer.allocate(18).order(ByteOrder.LITTLE_ENDIAN);
        fmt.putShort((short) 0x0166); // wFormatTag = XMA2
        fmt.putShort((short) channels);
        fmt.putInt(sampleRate);
        fmt.putInt(0);                 // nAvgBytesPerSec
        fmt.putShort((short) 2048);    // nBlockAlign
        fmt.putShort((short) 16);      // wBitsPerSample
        fmt.putShort((short) 0);       // cbSize

        byte[] data = new byte[2048];  // one all-zero packet

        int riffSize = 4 + (8 + fmt.capacity()) + (8 + data.length);
        ByteBuffer bb = ByteBuffer.allocate(8 + riffSize).order(ByteOrder.LITTLE_ENDIAN);
        bb.put("RIFF".getBytes());
        bb.putInt(riffSize);
        bb.put("WAVE".getBytes());
        bb.put("fmt ".getBytes());
        bb.putInt(fmt.capacity());
        bb.put(fmt.array());
        bb.put("data".getBytes());
        bb.putInt(data.length);
        bb.put(data);
        return bb.array();
    }
}
