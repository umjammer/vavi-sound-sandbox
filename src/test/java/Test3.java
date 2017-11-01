/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.net.URL;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.SourceDataLine;


/**
 * line.
 * 
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/06/11 umjammer initial version <br>
 */
public class Test3 {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        URL url = new URL(args[0]);
        for (AudioFileFormat.Type type : AudioSystem.getAudioFileTypes()) {
                System.err.println(type);
        }
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(url);
        AudioFormat audioFormat = audioInputStream.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
    line.addLineListener(event -> {
            if (event.getType().equals(LineEvent.Type.START)) {
            }
            if (event.getType().equals(LineEvent.Type.STOP)) {
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
