/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mocha;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;

import mocha.sound.Maximizer;
import mocha.sound.SoundConstants;
import mocha.sound.SoundReadable;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.getLogger;


/**
 * MochaSynthesizer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/30 umjammer initial version <br>
 */
public class MochaAudioInpuStream extends InputStream {

    private static final Logger logger = getLogger(MochaAudioInpuStream.class.getName());

    boolean signed = true;
    boolean big_endian = true;
    int sample_size_byte = 2;
    byte[] listBuffer;
    int listBufferIndex;
    Maximizer maximizer;
    long index_start;
    long index_end;
    long index;
    long start;

    public MochaAudioInpuStream(SoundReadable readable) throws IOException {
        init(readable);
    }

    void init(SoundReadable readable) throws IOException {
logger.log(DEBUG, "readable: " + readable.length());
        start = System.currentTimeMillis();
        double volume = Math.pow(2, sample_size_byte * 8 - 1) - 1;
        maximizer = new Maximizer(readable, volume);
        index_start = (long) SoundConstants.SAMPLE_RATE * maximizer.getChannel();
        index_end = index_start + maximizer.length();
        index = 0;

        // byteBuffer = ByteBuffer.allocate(8);
        listBuffer = new byte[sample_size_byte];
        listBufferIndex = sample_size_byte;
    }

    @Override
    public int read() throws IOException {
        // if (list.isEmpty()) {
        if (listBufferIndex >= listBuffer.length) {
            double value;
            if (index >= index_start && index < index_end) {
                value = maximizer.read();
            } else {
                value = 0;
            }
            index++;
            if (index % (SoundConstants.SAMPLE_RATE * 2 * 5) == 0) {
                logger.log(Level.TRACE, "wrote " + (index / SoundConstants.SAMPLE_RATE / 2) + " sec");
            }
            ByteBuffer byteBuffer = ByteBuffer.allocate(8);
            byteBuffer.putLong((long) value);
            byte[] array = byteBuffer.array();
            if (sample_size_byte >= 0)
                System.arraycopy(array, 0 - sample_size_byte + 8, listBuffer, 0, sample_size_byte);
            listBufferIndex = 0;
        }
        int ret = Byte.toUnsignedInt(listBuffer[listBufferIndex++]);
//logger.log(Level.TRACE, "wav:" + ret);
        return ret;
    }

    public long length() {
        return maximizer.length() / maximizer.getChannel() + (long) SoundConstants.SAMPLE_RATE * 2;
    }

    public AudioFormat getFormat() {
        return new AudioFormat(SoundConstants.SAMPLE_RATE, sample_size_byte * 8, maximizer.getChannel(), signed, big_endian);
    }

    @Override
    public void close() throws IOException {
        maximizer.terminate();
//logger.log(Level.TRACE, "total time:" + ((System.currentTimeMillis() - start) / 1000));
    }
}
