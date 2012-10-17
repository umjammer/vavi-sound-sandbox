/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;


/**
 * line.
 * 
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/06/11 umjammer initial version <br>
 */
public class Test2 {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        // speaker
        AudioFormat targetFormat = new AudioFormat(44100, 16, 2, true, false);
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, targetFormat);
        TargetDataLine target = (TargetDataLine) AudioSystem.getLine(targetInfo);
        target.open(targetFormat);
        target.start();
        AudioInputStream stream = new AudioInputStream(target);

        // microphone
        AudioFormat sourceFormat = new AudioFormat(44100, 16, 2, true, false);
        DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, sourceFormat);
        SourceDataLine source = (SourceDataLine) AudioSystem.getLine(sourceInfo);
        source.open(sourceFormat);
        source.start();

        byte[] buf = new byte[8192];
        while (true) {
            int r = stream.read(buf);
            if (r < 0) {
                break;
            }
            source.write(buf, 0, r);
        }
        
        source.drain();
        source.stop();
        source.close();
    }
}

/* */
