/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.xma;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.spi.FormatConversionProvider;

import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;


/**
 * WmaFormatConversionProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260707 nsano initial version <br>
 */
public class XmaFormatConversionProvider extends FormatConversionProvider {

    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        return new AudioFormat.Encoding[] {XmaEncoding.XMA, AudioFormat.Encoding.PCM_SIGNED};
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        return new AudioFormat.Encoding[] {XmaEncoding.XMA, AudioFormat.Encoding.PCM_SIGNED};
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding() instanceof XmaEncoding) {
            return new AudioFormat.Encoding[] {AudioFormat.Encoding.PCM_SIGNED};
        } else {
            return new AudioFormat.Encoding[0];
        }
    }

    @Override
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding() instanceof XmaEncoding && targetEncoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
            int channels = sourceFormat.getChannels();
            float sampleRate = sourceFormat.getSampleRate();
            return new AudioFormat[] {
                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                            sampleRate,
                            16,                                              // sample size in bits
                            channels,
                            channels == NOT_SPECIFIED ? NOT_SPECIFIED : channels * 2, // frame size
                            sampleRate,                                      // frame rate
                            false)                                           // little endian
            };
        } else {
            return new AudioFormat[0];
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding, AudioInputStream sourceStream) {
        if (isConversionSupported(targetEncoding, sourceStream.getFormat())) {
            AudioFormat[] formats = getTargetFormats(targetEncoding, sourceStream.getFormat());
            if (formats != null && formats.length > 0) {
                return getAudioInputStream(formats[0], sourceStream);
            } else {
                throw new IllegalArgumentException("target format not found");
            }
        } else {
            throw new IllegalArgumentException("conversion not supported");
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream) {
        if (isConversionSupported(targetFormat, sourceStream.getFormat())) {
            AudioFormat sourceFormat = sourceStream.getFormat();
            if (sourceFormat.equals(targetFormat)) {
                return sourceStream;
            } else if (sourceFormat.getEncoding() instanceof XmaEncoding
                    && targetFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
                try {
                    return new Xma2PcmAudioInputStream(sourceStream, targetFormat, NOT_SPECIFIED);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                throw new IllegalArgumentException("unable to convert " + sourceFormat + " to " + targetFormat);
            }
        } else {
            throw new IllegalArgumentException("conversion not supported");
        }
    }
}
