/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.opl3;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;


/**
 * Opl3FormatConversionProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201022 nsano initial version <br>
 */
public class Opl3FormatConversionProvider extends FormatConversionProvider {

    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        return new AudioFormat.Encoding[] {Opl3Encoding.MID, Opl3Encoding.DRO1, Opl3Encoding.DRO2};
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        return new AudioFormat.Encoding[] {AudioFormat.Encoding.PCM_SIGNED};
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding() instanceof Opl3Encoding) {
            return new AudioFormat.Encoding[] {AudioFormat.Encoding.PCM_SIGNED};
        } else {
            return new AudioFormat.Encoding[0];
        }
    }

    @Override
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding() instanceof Opl3Encoding && targetEncoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
            // TODO signed, endian should be free (means add more 3 patterns)
            return new AudioFormat[] {
                    new AudioFormat(sourceFormat.getSampleRate(),
                            16,           // sample size in bits
                            sourceFormat.getChannels(),
                            true,                // signed
                            false)                      // little endian (for PCM wav)
            };
        } else {
            return new AudioFormat[0];
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding, AudioInputStream sourceStream) {
        try {
            if (isConversionSupported(targetEncoding, sourceStream.getFormat())) {
                AudioFormat[] formats = getTargetFormats(targetEncoding, sourceStream.getFormat());
                if (formats != null && formats.length > 0) {
                    AudioFormat sourceFormat = sourceStream.getFormat();
                    AudioFormat targetFormat = formats[0];
                    if (sourceFormat.equals(targetFormat)) {
                        return sourceStream;
                    } else if (sourceFormat.getEncoding() instanceof Opl3Encoding && targetFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
                        return new Opl3ToPcmAudioInputStream(sourceStream, targetFormat, AudioSystem.NOT_SPECIFIED, sourceFormat);
                    } else if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) && targetFormat.getEncoding() instanceof Opl3Encoding) {
                        throw new IllegalArgumentException("unable to convert " + sourceFormat + " to " + targetFormat);
                    } else {
                        throw new IllegalArgumentException("unable to convert " + sourceFormat + " to " + targetFormat.toString());
                    }
                } else {
                    throw new IllegalArgumentException("target format not found");
                }
            } else {
                throw new IllegalArgumentException("conversion not supported");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream) {
        try {
            if (isConversionSupported(targetFormat, sourceStream.getFormat())) {
                AudioFormat[] formats = getTargetFormats(targetFormat.getEncoding(), sourceStream.getFormat());
                if (formats != null && formats.length > 0) {
                    AudioFormat sourceFormat = sourceStream.getFormat();
                    if (sourceFormat.equals(targetFormat)) {
                        return sourceStream;
                    } else if (sourceFormat.getEncoding() instanceof Opl3Encoding &&
                            targetFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
                        return new Opl3ToPcmAudioInputStream(sourceStream, targetFormat, AudioSystem.NOT_SPECIFIED, sourceFormat);
                    } else if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) && targetFormat.getEncoding() instanceof Opl3Encoding) {
                        throw new IllegalArgumentException("unable to convert " + sourceFormat + " to " + targetFormat);
                    } else {
                        throw new IllegalArgumentException("unable to convert " + sourceFormat + " to " + targetFormat);
                    }
                } else {
                    throw new IllegalArgumentException("target format not found");
                }
            } else {
                throw new IllegalArgumentException("conversion not supported");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}

/* */
