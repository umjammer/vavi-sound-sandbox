/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.ilbc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineEvent.Type;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;
import static vavi.sound.SoundUtil.volume;


@PropsEntity(url = "file:local.properties")
class IlbcFormatConversionProviderTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "ilbc")
    String ilbc = "src/test/resources/ilbc/sample-30.ilbc";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    static double volume = Double.parseDouble(System.getProperty("vavi.test.volume",  "0.2"));

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test1() throws Exception {
        Path in = Path.of(ilbc);
Debug.println(in);

        AudioFormat inFormat = new AudioFormat(
                IlbcEncoding.iLBC,
                8000,
                16,
                1,
                2,
                8000,
                false);

        AudioFormat linFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                8000,
                16,
                1,
                2,
                8000,
                false);
Debug.println(linFormat);

        var ais = new AudioInputStream(Files.newInputStream(in), inFormat, NOT_SPECIFIED);
        Clip clip = AudioSystem.getClip();
        CountDownLatch cdl = new CountDownLatch(1);
        clip.addLineListener(e -> { Debug.println(e.getType()); if (e.getType().equals(Type.STOP)) cdl.countDown(); });
        clip.open(AudioSystem.getAudioInputStream(linFormat, ais));
        volume(clip, volume);
        clip.start();
        cdl.await();
        clip.drain();
        clip.close();
    }
}