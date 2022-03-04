/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import vavi.util.Debug;

import static vavi.sound.SoundUtil.volume;


/**
 * line.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/06/11 umjammer initial version <br>
 */
class Test3 {

//    static final String inFile = "/Users/nsano/Music/0/Mists of Time - 4T.ogg";
//    static final String inFile = "/Users/nsano/Music/0/11 - Blockade.flac";
//    static final String inFile = "/Users/nsano/Music/0/11 - Blockade.m4a"; // ALAC
//    static final String inFile = "/Users/nsano/Music/0/rc.wav";
//    static final String inFile = "/Users/nsano/Music/0/Cyndi Lauper-Time After Time.m4a"; // AAC
    static final String inFile = "tmp/hoshiF.opus";
//    static final String inFile = "/Users/nsano/Music/iTunes/iTunes Music/NAMCO/Ace Combat 04 Shattered Skies Original Sound Tracks/1-11 Blockade.mp3";

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        for (AudioFileFormat.Type type : AudioSystem.getAudioFileTypes()) {
System.err.println(type);
        }
        AudioInputStream originalAudioInputStream = AudioSystem.getAudioInputStream(new File(inFile).toURI().toURL());
        Test3 app = new Test3();
        app.play(originalAudioInputStream);
    }

    /** */
    void play(AudioInputStream originalAudioInputStream) throws Exception {
        AudioFormat originalAudioFormat = originalAudioInputStream.getFormat();
Debug.println(originalAudioFormat);
        AudioFormat targetAudioFormat = new AudioFormat( //PCM
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
        line.addLineListener(event -> {
            if (event.getType().equals(LineEvent.Type.START)) {
Debug.println("play");
            }
            if (event.getType().equals(LineEvent.Type.STOP)) {
Debug.println("done");
            }
        });

        line.open(audioFormat);
        byte[] buf = new byte[line.getBufferSize()];
        volume(line, .2d);
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
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(file).toURI().toURL());
        AudioFormat audioFormat = audioInputStream.getFormat();
        play(audioInputStream);
} catch (Throwable t) {
  Debug.println("file: " + t);
  t.printStackTrace();
  throw t;
}
    }
}

/* */
