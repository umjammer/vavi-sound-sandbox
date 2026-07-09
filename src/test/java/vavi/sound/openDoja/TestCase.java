/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.openDoja;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import opendoja.audio.mld.MLD;
import opendoja.audio.mld.MLDPlayer;
import opendoja.audio.mld.MLDPlayerEvent;
import opendoja.audio.mld.SamplerProvider;
import opendoja.audio.mld.ma3.MA3SamplerProvider;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static vavi.sound.SoundUtil.volume;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-06-30 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class TestCase {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @Property
    String mfi = "src/test/resources/test.mld";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

Debug.println("volume: " + volume);
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test1() throws Exception {
        Path path = Path.of(mfi);
Debug.print(mfi);

        float sampleRate = 48000F;
        AudioFormat audioFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate,
                16,
                2,
                4,
                sampleRate,
                false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);

        try (InputStream is = new BufferedInputStream(Files.newInputStream(path))) {
            MLD mld = new MLD(is);
            SamplerProvider provider = new MA3SamplerProvider();
            MLDPlayer mldPlayer = new MLDPlayer(mld, provider, sampleRate);
            mldPlayer.setLoopEnabled(false);
            mldPlayer.setPlaybackEventsEnabled(true);

            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            try {
                line.open(audioFormat);
                line.addLineListener(ev -> Debug.println(ev.getType()));
                line.start();
                volume(line, volume);

                int frames = 4096;
                float[] samples = new float[frames * 2];
                byte[] buffer = new byte[frames * audioFormat.getFrameSize()];

                while (!mldPlayer.isFinished()) {
                    int rendered = mldPlayer.render(samples, 0, frames, (float) volume);
                    MLDPlayerEvent[] events = mldPlayer.getEvents();
                    for (MLDPlayerEvent event : events) {
                        Debug.println("event: " + event.type + ", time: " + event.time + ", data: " + event.data);
                    }
                    if (rendered == -1) {
                        break;
                    }
                    if (rendered == 0) {
                        continue;
                    }

                    int length = toPcm16le(samples, rendered, buffer);
                    line.write(buffer, 0, length);
                }

                line.drain();
            } finally {
                line.stop();
                line.close();
            }
        }
    }

    private static int toPcm16le(float[] samples, int frames, byte[] buffer) {
        for (int i = 0; i < frames * 2; i++) {
            int sample = Math.round(samples[i] * Short.MAX_VALUE);
            sample = Math.clamp(sample, Short.MIN_VALUE, Short.MAX_VALUE);
            int offset = i * 2;
            buffer[offset] = (byte) sample;
            buffer[offset + 1] = (byte) (sample >>> 8);
        }
        return frames * 4;
    }
}
