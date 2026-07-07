/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.resampling;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.spi.FormatConversionProvider;

import vavi.sound.pcm.equalizing.sse.Equalizer;


/**
 * SSEEqualizingProvider.
 * <p>
 * Runs a PCM stream through the Shibatch Super Equalizer. Tag the source stream
 * with {@link SSEEncoding#SSE_EQUALIZE} and convert it to
 * {@link AudioFormat.Encoding#PCM_SIGNED} to obtain the equalized audio.
 * <p>
 * By default a flat (identity) band table is used; subclasses may override
 * {@link #makeTable(Equalizer, float)} to configure band gains and parametric
 * boosts. The whole source is read up front and processed in one pass.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 250707 nsano initial version <br>
 * @see Equalizer
 */
public class SSEEqualizingProvider extends FormatConversionProvider {

    /** window length bits passed to {@link Equalizer#Equalizer(int)} */
    protected static final int WB = 14;

    private static boolean isPcm16(AudioFormat format) {
        return format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) &&
                format.getSampleSizeInBits() == 16;
    }

    /**
     * Configures the equalizer band table. The default is a flat identity
     * response; override to apply gains or parametric boosts.
     *
     * @param equ the equalizer to configure
     * @param fs  the sample rate in Hz
     */
    protected void makeTable(Equalizer equ, float fs) {
        double[] gains = new double[Equalizer.getBandsCount() + 1];
        Arrays.fill(gains, 1.0);
        equ.equ_makeTable(gains, gains, new ArrayList<>(), fs);
    }

    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        return new AudioFormat.Encoding[] {SSEEncoding.SSE_EQUALIZE};
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        return new AudioFormat.Encoding[] {AudioFormat.Encoding.PCM_SIGNED};
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding() instanceof SSEEncoding) {
            return new AudioFormat.Encoding[] {AudioFormat.Encoding.PCM_SIGNED};
        } else {
            return new AudioFormat.Encoding[0];
        }
    }

    @Override
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        if (targetEncoding.equals(AudioFormat.Encoding.PCM_SIGNED) &&
                sourceFormat.getEncoding() instanceof SSEEncoding &&
                sourceFormat.getSampleSizeInBits() == 16) {
            return new AudioFormat[] {
                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                            sourceFormat.getSampleRate(),
                            16,
                            sourceFormat.getChannels(),
                            sourceFormat.getFrameSize(),
                            sourceFormat.getFrameRate(),
                            sourceFormat.isBigEndian())
            };
        } else {
            return new AudioFormat[0];
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding, AudioInputStream sourceStream) {
        AudioFormat[] formats = getTargetFormats(targetEncoding, sourceStream.getFormat());
        if (formats.length == 0) {
            throw new IllegalArgumentException("conversion not supported: " + sourceStream.getFormat());
        }
        return getAudioInputStream(formats[0], sourceStream);
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream) {
        AudioFormat sourceFormat = sourceStream.getFormat();
        if (!(sourceFormat.getEncoding() instanceof SSEEncoding) || !isPcm16(targetFormat)) {
            throw new IllegalArgumentException("unable to convert " + sourceFormat + " to " + targetFormat);
        }
        int nch = sourceFormat.getChannels();
        float fs = sourceFormat.getSampleRate();
        try {
            byte[] pcm = sourceStream.readAllBytes();

            Equalizer equ = new Equalizer(WB);
            makeTable(equ, fs);
            int nsamples = pcm.length / (2 * nch); // 16bit frames
            equ.equ_modifySamples(pcm, nsamples, nch, 16);
            equ.equ_quit();

            AudioFormat outFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    fs,
                    16,
                    nch,
                    sourceFormat.getFrameSize(),
                    sourceFormat.getFrameRate(),
                    sourceFormat.isBigEndian());
            return new AudioInputStream(new ByteArrayInputStream(pcm), outFormat, nsamples);
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
