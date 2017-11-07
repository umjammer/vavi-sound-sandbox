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
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;


/**
 * clip.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/06/11 umjammer initial version <br>
 */
public class Test1 {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        URL clipURL = new URL(args[0]);
        for (AudioFileFormat.Type type : AudioSystem.getAudioFileTypes()) {
                System.err.println(type);
        }
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(clipURL);
        AudioFormat audioFormat = audioInputStream.getFormat();
        DataLine.Info info = new DataLine.Info(Clip.class, audioFormat, AudioSystem.NOT_SPECIFIED);
        Clip clip = (Clip) AudioSystem.getLine(info);
        clip.addLineListener(event -> {
            if (event.getType().equals(LineEvent.Type.START)) {
            }
            if (event.getType().equals(LineEvent.Type.STOP)) {
            }
        });
        clip.open(audioInputStream);
        clip.start();
    }
}

/* */
