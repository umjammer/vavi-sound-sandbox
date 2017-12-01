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

import org.junit.Ignore;


/**
 * line.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/06/11 umjammer initial version <br>
 */
@Ignore
public class Test3 {

//    static final String inFile = "/Users/nsano/Music/0/Mists of Time - 4T.ogg";
//    static final String inFile = "/Users/nsano/Music/0/11 - Blockade.flac";
//    static final String inFile = "/Users/nsano/Music/0/11 - Blockade.m4a"; // ALAC
    static final String inFile = "/Users/nsano/Music/0/rc.wav";
//    static final String inFile = "/Users/nsano/Music/0/Cyndi Lauper-Time After Time.m4a"; // AAC
//    static final String inFile = "tmp/hoshiF.opus";

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        for (AudioFileFormat.Type type : AudioSystem.getAudioFileTypes()) {
            System.err.println(type);
        }
//        URL url = new URL(args[0]);
//      AudioInputStream originalAudioInputStream = AudioSystem.getAudioInputStream(url);
        AudioInputStream originalAudioInputStream = AudioSystem.getAudioInputStream(new File(inFile).toURI().toURL());
        AudioFormat originalAudioFormat = originalAudioInputStream.getFormat();
System.err.println(originalAudioFormat);
        AudioFormat targetAudioFormat = new AudioFormat(
            originalAudioFormat.getSampleRate(),
            16,
            originalAudioFormat.getChannels(),
            true,
            false);
System.err.println(targetAudioFormat);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(targetAudioFormat, originalAudioInputStream);
        AudioFormat audioFormat = audioInputStream.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.addLineListener(event -> {
            if (event.getType().equals(LineEvent.Type.START)) {
System.err.println("play");
            }
            if (event.getType().equals(LineEvent.Type.STOP)) {
System.err.println("done");
            }
        });

        byte[] buf = new byte[8192];
        line.open(audioFormat, buf.length);
        line.start();
        int r = 0;
        while (true) {
            r = audioInputStream.read(buf, 0, buf.length);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }
        line.drain();
        line.close();
    }
}

/* */
