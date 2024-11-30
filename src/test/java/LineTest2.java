/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import vavi.util.Debug;

import static vavi.sound.SoundUtil.volume;

import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * line.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/06/11 umjammer initial version <br>
 */

@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file:local.properties")
class LineTest2 {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    static {
        System.setProperty("vavi.util.logging.VaviFormatter.extraClassMethod",
                           "vavi\\.sound\\.DebugInputStream#\\w+");
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @Property(name = "line2.test")
    String inFile = "src/test/resources/test.caf";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    /**
     * @param args use local.properties
     */
    public static void main(String[] args) throws Exception {
        LineTest2 app = new LineTest2();
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(app);
        }

        for (AudioFileFormat.Type type : AudioSystem.getAudioFileTypes()) {
System.err.println(type);
        }
        AudioInputStream originalAudioInputStream = AudioSystem.getAudioInputStream(new File(app.inFile).toURI().toURL());
        app.play(originalAudioInputStream);
    }

    /** play the audio using line */
    void play(AudioInputStream originalAudioInputStream) throws Exception {
        AudioFormat originalAudioFormat = originalAudioInputStream.getFormat();
Debug.println(originalAudioFormat);
        AudioFormat targetAudioFormat = new AudioFormat( // PCM
            originalAudioFormat.getSampleRate(),
            16,
            originalAudioFormat.getChannels(),
            true,
            false);
Debug.println(targetAudioFormat);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(targetAudioFormat, originalAudioInputStream);
        AudioFormat audioFormat = audioInputStream.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
Debug.println(line.getClass().getName());
        line.addLineListener(event -> Debug.println(event.getType()));

        line.open(audioFormat);
        byte[] buf = new byte[line.getBufferSize()];
        volume(line, volume);
        line.start();
        int r = 0;
        while (true) {
            r = audioInputStream.read(buf, 0, buf.length);
            if (r < 0) {
                break;
            }
//Debug.println("line: " + line.available());
            line.write(buf, 0, r);
        }
        line.drain();
        line.close();
    }

    @ParameterizedTest
    @DisplayName("test many types")
    @ValueSource(strings = {
        "src/test/resources/speex.ogg",
        "src/test/resources/test.ogg",
        "src/test/resources/alac.m4a",
        "src/test/resources/test.flac",
        "src/test/resources/test.m4a",
        "src/test/resources/test.mp3",
        "src/test/resources/test.opus",
    })
    void test(String file) throws Exception {
Debug.println("------------------------------------------ " + file + " ------------------------------------------------");
try {
        InputStream is = new BufferedInputStream(Files.newInputStream(Paths.get(file)));
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(is);
//        AudioInputStream audioInputStream = dummy(is);
        play(audioInputStream);
} catch (Throwable t) {
  Debug.println("file: " + file);
  Debug.printStackTrace(t);
  throw t;
}
    }

    /** simulate jdk {@link AudioSystem#getAudioInputStream} method */
    @SuppressWarnings({ "unchecked", "rawtypes", "restriction" })
    static AudioInputStream dummy(InputStream stream) throws UnsupportedAudioFileException, IOException {
        List providers = com.sun.media.sound.JDK13Services.getProviders(AudioFileReader.class);
providers.forEach(System.err::println);
        AudioInputStream audioStream = null;

        for (Object provider : providers) {
            AudioFileReader reader = (AudioFileReader) provider;
Debug.println("--------- " + reader.getClass().getName());
            try {
                audioStream = reader.getAudioInputStream(stream); // throws IOException
                break;
            } catch (UnsupportedAudioFileException e) {
                Debug.println("ERROR: " + e.getMessage());
            }
        }

        if( audioStream==null ) {
            throw new UnsupportedAudioFileException("could not get audio input stream from input stream");
        } else {
            return audioStream;
        }
    }

    @Test
    @Disabled("for just fix #7, not needed any more")
    @DisplayName("for just fix #7")
    void test2() throws Exception {
        InputStream is = new BufferedInputStream(Files.newInputStream(Paths.get("src/test/resources/test.mp3")));
        AudioInputStream audioInputStream = new MpegAudioFileReader().getAudioInputStream(is);
        play(audioInputStream);
    }
}
