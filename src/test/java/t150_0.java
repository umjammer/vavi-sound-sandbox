/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.File;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;


/**
 * resampling.
 * 
 * @see "http://www.tritonus.org/"
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 051015 nsano initial version <br>
 */
public class t150_0 {

    /**
     * t150_0
     */
    public static void main(String[] args) throws Exception {

        int outSamplingRate = 8000;

        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new File(args[0]));
        AudioFormat inAudioFormat = sourceAis.getFormat();
System.err.println("IN: " + inAudioFormat);
        
        ByteOrder outByteOrder = ByteOrder.LITTLE_ENDIAN;
        AudioFormat outAudioFormat1 = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            outSamplingRate,
            16,
            2,
            4,
            outSamplingRate,
            outByteOrder.equals(ByteOrder.BIG_ENDIAN));
System.err.println("OUT: " + outAudioFormat1);
System.err.println("OK: " + AudioSystem.isConversionSupported(outAudioFormat1, inAudioFormat));

        AudioInputStream firstAis = AudioSystem.getAudioInputStream(outAudioFormat1, sourceAis);


        AudioFormat outAudioFormat2 = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            outSamplingRate,
            16,
            1,
            2,
            outSamplingRate,
            outByteOrder.equals(ByteOrder.BIG_ENDIAN));
System.err.println("OUT: " + outAudioFormat2);
System.err.println("OK: " + AudioSystem.isConversionSupported(outAudioFormat2, outAudioFormat1));

        AudioInputStream secondAis = AudioSystem.getAudioInputStream(outAudioFormat2, firstAis);


        int r = AudioSystem.write(secondAis, AudioFileFormat.Type.WAVE, new File(args[1]));
System.err.println("RESULT: " + r);
    }
}

/* */
