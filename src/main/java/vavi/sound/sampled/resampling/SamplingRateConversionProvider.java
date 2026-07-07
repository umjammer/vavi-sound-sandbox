/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.resampling;

import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;


/**
 * Base class for the PCM sample-rate-conversion {@link FormatConversionProvider}s.
 * <p>
 * All of the wrapped resamplers operate on 16bit little-endian PCM, so the
 * supported conversion is {@code PCM_SIGNED 16bit -> PCM_SIGNED 16bit} where only
 * the sample rate differs. The advertised target sample rate is
 * {@link AudioSystem#NOT_SPECIFIED} so any output rate matches; the concrete rate
 * is taken from the {@link AudioFormat} passed to
 * {@link #getAudioInputStream(AudioFormat, AudioInputStream)}.
 * <p>
 * The underlying resamplers are single-channel (see their {@code TODO stereo}
 * notes); the interleaved byte stream is passed through as-is.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 250707 nsano initial version <br>
 */
abstract class SamplingRateConversionProvider extends FormatConversionProvider {

    /** Wrap the raw 16bit LE PCM stream with the concrete resampler. */
    protected abstract InputStream createStream(InputStream in, float inRate, float outRate) throws IOException;

    /** whether the given format is something our resamplers can read/write */
    private static boolean isPcm16(AudioFormat format) {
        return format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) &&
                format.getSampleSizeInBits() == 16;
    }

    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        return new AudioFormat.Encoding[] {AudioFormat.Encoding.PCM_SIGNED};
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        return new AudioFormat.Encoding[] {AudioFormat.Encoding.PCM_SIGNED};
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        if (isPcm16(sourceFormat)) {
            return new AudioFormat.Encoding[] {AudioFormat.Encoding.PCM_SIGNED};
        } else {
            return new AudioFormat.Encoding[0];
        }
    }

    @Override
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        if (targetEncoding.equals(AudioFormat.Encoding.PCM_SIGNED) && isPcm16(sourceFormat)) {
            // sample rate free (NOT_SPECIFIED): any output rate matches
            return new AudioFormat[] {
                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                            AudioSystem.NOT_SPECIFIED, // sample rate
                            16,                        // sample size in bits
                            sourceFormat.getChannels(),
                            sourceFormat.getFrameSize(),
                            AudioSystem.NOT_SPECIFIED, // frame rate
                            sourceFormat.isBigEndian())
            };
        } else {
            return new AudioFormat[0];
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding, AudioInputStream sourceStream) {
        // the target sample rate cannot be derived from the encoding alone
        throw new IllegalArgumentException("target sample rate must be specified via AudioFormat");
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream) {
        AudioFormat sourceFormat = sourceStream.getFormat();
        if (!isPcm16(sourceFormat) || !isPcm16(targetFormat)) {
            throw new IllegalArgumentException("unable to convert " + sourceFormat + " to " + targetFormat);
        }
        if (sourceFormat.getChannels() != targetFormat.getChannels()) {
            throw new IllegalArgumentException("channel count must match: " + sourceFormat + " -> " + targetFormat);
        }
        float inRate = sourceFormat.getSampleRate();
        float outRate = targetFormat.getSampleRate();
        if (inRate == outRate) {
            return sourceStream;
        }
        try {
            InputStream resampled = createStream(sourceStream, inRate, outRate);
            AudioFormat outFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    outRate,
                    16,
                    sourceFormat.getChannels(),
                    sourceFormat.getFrameSize(),
                    outRate,
                    sourceFormat.isBigEndian());
            return new AudioInputStream(resampled, outFormat, AudioSystem.NOT_SPECIFIED);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
