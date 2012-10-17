/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.alac;

import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import com.beatofthedrum.alacdecoder.AlacInputStream;


/**
 * Converts an ALAC bitstream into a PCM 16bits/sample audio stream.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 111022 nsano initial version <br>
 */
class Alac2PcmAudioInputStream extends AudioInputStream {

    /**
     * Constructor.
     * 
     * @param in the underlying input stream.
     * @param format the target format of this stream's audio data.
     * @param length the length in sample frames of the data in this stream.
     */
    public Alac2PcmAudioInputStream(InputStream in, AudioFormat format, long length) {
        super(new AlacInputStream(in), format, length);
    }
}

/* */
