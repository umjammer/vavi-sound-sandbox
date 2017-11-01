/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.resampling;

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioFileFormat.Type;

import org.junit.Test;

import vavi.sound.sampled.MonauralInputFilter;
import vavi.util.Debug;

import static org.junit.Assert.*;


/**
 * SampleRateConversionProviderTest. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060726 nsano initial version <br>
 */
public class SampleRateConversionProviderTest {

    String inFile = "C:\\Documents and Settings\\sano-n\\My Documents\\My Music\\0\\大塚 愛 - さくらんぼ.wav";
    String outFile = "out.wav";

    /**
     *  
     * <ul>
     *  <li>→ mono に tritonus_remaining_###.jar が必要 
     *  <li>eclipse では jar の順位がしたの方から plugin が機能している？？？
     * </ul>
     *  
     */
    @Test
    public void test1() throws Exception {

for (Type type : AudioSystem.getAudioFileTypes()) {
    System.err.println("type: " + type);
}

        //
        int outSamplingRate = 8000;

        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new File(inFile));

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
