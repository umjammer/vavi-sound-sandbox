/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.opl3;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.sound.SoundUtil.volume;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-03-04 nsano initial version <br>
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file:local.properties")
public class TestCase {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property
    String opl3;

    @Property(name = "vavi.test.volume.midi")
    double volume = 0.2;

    static boolean onIde = System.getProperty("vavi.test", "").equals("ide");
    static long time = onIde ? 1000 * 1000 : 5 * 1000;

    @BeforeEach
    void setupEach() throws IOException {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

Debug.println("volume: " + volume + ", use opl midi?: " + System.getProperty("vavi.sound.opl3.MidiFile"));
    }

    @Test
    @DisplayName("play via raw api")
    void test1() throws Exception {
        Path path = Path.of(opl3);
        InputStream is = new BufferedInputStream(Files.newInputStream(path));
        AudioFormat.Encoding encoding = Opl3Player.getEncoding(is);
        Opl3Player player = Opl3Player.getPlayer(encoding);
        player.setProperties(Map.of("uri", path.toUri()));
        player.load(is);

        AudioFormat audioFormat = new AudioFormat(
                Opl3Player.opl3.getSampleRate(),
                16,
                Opl3Player.opl3.getChannels(),
                true,
                false);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.addLineListener(ev -> Debug.println(ev.getType()));
        line.start();

        volume(line, volume);

        while (player.update()) {
            double sec = 1.0 / player.getRefresh();

            byte[] buf = player.read(4 * (int) (Opl3Player.opl3.getSampleRate() * sec));
            line.write(buf, 0, buf.length);
        }
        for (int wait = 0; wait < 30; ++wait) {
            double sec = 0.1;

            byte[] buf = player.read(4 * (int) (Opl3Player.opl3.getSampleRate() * sec));
            line.write(buf, 0, buf.length);
        }

        line.drain();
        line.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "src/test/resources/opl3/ARAB.bam",
            "src/test/resources/opl3/16.lds",
            "src/test/resources/opl3/2001.MKJ",
            "src/test/resources/opl3/BEGIN.KSM",
            "src/test/resources/opl3/CHILD1.XSM",
            "src/test/resources/opl3/adlibsp.s3m",
            "src/test/resources/opl3/WONDERIN.WLF"
    })
    @DisplayName("play via raw api multiple formats")
    void test2(String filename) throws Exception {
        Path path = Path.of(filename);
        InputStream is = new BufferedInputStream(Files.newInputStream(path));
        AudioFormat.Encoding encoding = Opl3Player.getEncoding(is);
        vavi.util.Debug.println(path.getFileName() + " encoding: " + encoding);
        Opl3Player player = Opl3Player.getPlayer(encoding);
        player.setProperties(Map.of("uri", path.toUri()));
        player.load(is);

        AudioFormat audioFormat = new AudioFormat(
                Opl3Player.opl3.getSampleRate(),
                16,
                Opl3Player.opl3.getChannels(),
                true,
                false);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.addLineListener(ev -> Debug.println(ev.getType()));
        line.start();

        volume(line, volume);

        long start = System.currentTimeMillis();
        while (player.update()) {
            double sec = 1.0 / player.getRefresh();

            byte[] buf = player.read(4 * (int) (Opl3Player.opl3.getSampleRate() * sec));
            line.write(buf, 0, buf.length);
            if (System.currentTimeMillis() - start > time) {
                break;
            }
        }
        for (int wait = 0; wait < 10; ++wait) {
            double sec = 0.1;

            byte[] buf = player.read(4 * (int) (Opl3Player.opl3.getSampleRate() * sec));
            line.write(buf, 0, buf.length);
        }

        line.drain();
        line.close();
    }
}
