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
import vavi.sound.opl3.Opl3Player.FileType;
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
    static long time = onIde ? 1000 * 1000 : 10 * 1000;

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
        AudioFormat.Encoding encoding = FileType.getEncoding(is);
        Opl3Player player = FileType.getPlayer(encoding);
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
}
