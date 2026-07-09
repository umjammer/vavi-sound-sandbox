/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.resampling;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.spi.FormatConversionProvider;

import vavi.sound.pcm.equalizing.Normalizer;


/**
 * KohnNormalizingProvider.
 * <p>
 * Peak-normalizes a PCM stream using Michael Kohn's normalizer. Tag the source
 * stream with {@link KohnEncoding#KOHN_NORMALIZE} and convert it to
 * {@link AudioFormat.Encoding#PCM_SIGNED} to obtain the normalized audio.
 * <p>
 * Normalization is a two-pass operation (scan for the peak, then scale), so the
 * whole source is read up front.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 250707 nsano initial version <br>
 * @see Normalizer
 */
public class KohnNormalizingProvider extends FormatConversionProvider {

    @Override
    public boolean isConversionSupported(AudioFormat targetFormat, AudioFormat sourceFormat) {
        if (!Boolean.parseBoolean(System.getProperty("vavi.sound.sampled.spi.kohn", "false")))
            return false;

        return super.isConversionSupported(targetFormat, sourceFormat);
    }

    private static boolean isSupportedPcm(AudioFormat format) {
        int bits = format.getSampleSizeInBits();
        return format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) && (bits == 8 || bits == 16);
    }

    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        return new AudioFormat.Encoding[] {KohnEncoding.KOHN_NORMALIZE};
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        return new AudioFormat.Encoding[] {AudioFormat.Encoding.PCM_SIGNED};
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding() instanceof KohnEncoding) {
            return new AudioFormat.Encoding[] {AudioFormat.Encoding.PCM_SIGNED};
        } else {
            return new AudioFormat.Encoding[0];
        }
    }

    @Override
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        if (targetEncoding.equals(AudioFormat.Encoding.PCM_SIGNED) &&
                sourceFormat.getEncoding() instanceof KohnEncoding &&
                (sourceFormat.getSampleSizeInBits() == 8 || sourceFormat.getSampleSizeInBits() == 16)) {
            return new AudioFormat[] {
                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                            sourceFormat.getSampleRate(),
                            sourceFormat.getSampleSizeInBits(),
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
        if (!(sourceFormat.getEncoding() instanceof KohnEncoding) || !isSupportedPcm(targetFormat)) {
            throw new IllegalArgumentException("unable to convert " + sourceFormat + " to " + targetFormat);
        }
        try {
            byte[] pcm = sourceStream.readAllBytes();
            Normalizer.normalize(pcm, sourceFormat.getSampleSizeInBits());
            AudioFormat outFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    sourceFormat.getSampleRate(),
                    sourceFormat.getSampleSizeInBits(),
                    sourceFormat.getChannels(),
                    sourceFormat.getFrameSize(),
                    sourceFormat.getFrameRate(),
                    sourceFormat.isBigEndian());
            long frames = pcm.length / outFormat.getFrameSize();
            return new AudioInputStream(new ByteArrayInputStream(pcm), outFormat, frames);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
