/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;


/**
 * line. (mic -> speaker)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/06/11 umjammer initial version <br>
 */
public class Test2 {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        // microphone
        AudioFormat targetFormat = new AudioFormat(44100, 16, 2, true, false);
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, targetFormat);
System.err.println(targetInfo);
        TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(targetInfo);
        microphone.open(targetFormat);
        microphone.start();
        AudioInputStream stream = new AudioInputStream(microphone);

        // speaker
        AudioFormat sourceFormat = new AudioFormat(44100, 16, 2, true, false);
        DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, sourceFormat);
System.err.println(sourceInfo);
        SourceDataLine speaker = (SourceDataLine) AudioSystem.getLine(sourceInfo);
        speaker.open(sourceFormat);
FloatControl gainControl = (FloatControl) speaker.getControl(FloatControl.Type.MASTER_GAIN);
double gain = .2d; // number between 0 and 1 (loudest)
float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
gainControl.setValue(dB);
        speaker.start();

        byte[] buf = new byte[speaker.getBufferSize()];
        while (true) {
            int r = stream.read(buf);
            if (r < 0) {
                break;
            }
            speaker.write(buf, 0, r);
        }

        speaker.drain();
        speaker.stop();
        speaker.close();
    }
}

/* */
