/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.wma;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
import static vavi.sound.sampled.wma.WmaEncoding.WMA;
import static vavix.util.DelayedWorker.later;


/**
 * Tests for the WMA v1/v2 (ASF) sampled SPI.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260707 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class WmaFormatConversionProviderTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    static boolean onIde = System.getProperty("vavi.test", "").equals("ide");
    static long time = onIde ? 1000 * 1000 : 10 * 1000;

    /** committed real WMA v2 fixture */
    static final String FIXTURE = "src/test/resources/test.wma";

    /** an extra .wma to audition; supply your own via local.properties (key "wma") */
    @Property(name = "wma")
    String inFile = "src/test/resources/test.wma";

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    boolean inFileExists() {
        return Files.exists(Paths.get(inFile));
    }

    static boolean fixtureExists() {
        return Files.exists(Paths.get(FIXTURE));
    }

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @Test
    @DisplayName("recognises the WMA encoding via AudioSystem")
    @EnabledIf("fixtureExists")
    void recognisesEncoding() throws Exception {
        AudioInputStream ais = AudioSystem.getAudioInputStream(Paths.get(FIXTURE).toFile());
        assertEquals(WMA, ais.getFormat().getEncoding());
        assertEquals(44100f, ais.getFormat().getSampleRate());
        assertEquals(2, ais.getFormat().getChannels());
        ais.close();
    }

    @Test
    @DisplayName("decodes the fixture end-to-end to PCM16")
    @EnabledIf("fixtureExists")
    void decodesToPcm() throws Exception {
        Path path = Paths.get(FIXTURE);
        AudioInputStream sourceAis = new WmaAudioFileReader().getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)));
        AudioFormat in = sourceAis.getFormat();
Debug.println("IN: " + in);

        AudioFormat out = new AudioFormat(in.getSampleRate(), 16, in.getChannels(), true, false);
        assertTrue(AudioSystem.isConversionSupported(out, in));

        AudioInputStream pcm = new WmaFormatConversionProvider().getAudioInputStream(out, sourceAis);
        byte[] bytes = pcm.readAllBytes();
        pcm.close();
Debug.println("decoded pcm bytes: " + bytes.length);

        // 696320 samples/ch (ffmpeg) + 4096 priming = 700416 ; *2ch *2bytes
        assertEquals(700416 * 2 * 2, bytes.length);
        boolean nonSilent = false;
        for (byte b : bytes) {
            if (b != 0) {
                nonSilent = true;
                break;
            }
        }
        assertTrue(nonSilent, "decoded audio must not be all silence");
    }

    @Test
    @DisplayName("a non-ASF stream is unsupported and left untouched")
    void rejectsNonAsf() throws Exception {
        byte[] notAsf = "RIFF\0\0\0\0WAVEfmt ".getBytes();
        InputStream is = new BufferedInputStream(new ByteArrayInputStream(notAsf));
        int available = is.available();
        UnsupportedAudioFileException e = assertThrows(UnsupportedAudioFileException.class,
                () -> new WmaAudioFileReader().getAudioInputStream(is));
Debug.println(e.getMessage());
        assertEquals(available, is.available()); // spi must not consume the stream
    }

    @Test
    @DisplayName("play (local, supply local.properties key 'wma')")
    @EnabledIf("inFileExists")
    public void play() throws Exception {
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
}
