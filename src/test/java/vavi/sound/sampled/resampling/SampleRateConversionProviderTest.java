/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.resampling;

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;

import org.junit.Test;

import vavi.sound.sampled.MonauralInputFilter;
import vavi.util.Debug;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * SampleRateConversionProviderTest. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060726 nsano initial version <br>
 */
public class SampleRateConversionProviderTest {

    String inFile = "/Users/nsano/Music/0/Cyndi Lauper-Time After Time.m4a";
    String outFile = "tmp/out.wav";

    /**
     * <ul>
     *  <li>→ mono に tritonus_remaining_###.jar が必要 
     *  <li>eclipse では jar の順位がしたの方から plugin が機能している？？？
     * </ul>
     */
    @Test
    public void test1() throws Exception {

for (Type type : AudioSystem.getAudioFileTypes()) {
    System.err.println("type: " + type);
}

        //
        int outSamplingRate = 8000;

        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new File(inFile));
System.err.println("IN: " + sourceAis.getFormat());

        AudioFormat inAudioFormat = sourceAis.getFormat();
        AudioFormat outAudioFormat = new AudioFormat(
            inAudioFormat.getEncoding(),
            outSamplingRate,
            inAudioFormat.getSampleSizeInBits(),
            inAudioFormat.getChannels(),
            inAudioFormat.getFrameSize(),
            inAudioFormat.getFrameRate(),
            inAudioFormat.isBigEndian());

        assertTrue(AudioSystem.isConversionSupported(outAudioFormat, inAudioFormat));

        //
        AudioInputStream secondAis = AudioSystem.getAudioInputStream(outAudioFormat, sourceAis);
System.err.println("OUT: " + secondAis.getFormat());
for (Type type : AudioSystem.getAudioFileTypes(secondAis)) {
    System.err.println("type: " + type);
}
System.err.println("secondAis: " + secondAis.getFormat());
        AudioInputStream thirdAis = new MonauralInputFilter().doFilter(secondAis);
System.err.println("thirdAis: " + thirdAis.getFormat());
        AudioSystem.write(thirdAis, AudioFileFormat.Type.WAVE, new File(outFile));

        // 2. play
        AudioInputStream resultAis = AudioSystem.getAudioInputStream(new File(outFile));
        assertEquals(outSamplingRate, (int) resultAis.getFormat().getSampleRate());

        //
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, resultAis.getFormat());
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(resultAis.getFormat());
        line.addLineListener(new LineListener() {
            public void update(LineEvent ev) {
Debug.println(ev.getType());
                if (LineEvent.Type.STOP == ev.getType()) {
                    System.exit(0);
                }
            }
        });
        line.start();
        byte[] buf = new byte[1024];
        int l = 0;
FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
double gain = .2d; // number between 0 and 1 (loudest)
float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
gainControl.setValue(dB);

        while (resultAis.available() > 0) {
            l = resultAis.read(buf, 0, 1024);
            line.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();
    }
}

/* */
