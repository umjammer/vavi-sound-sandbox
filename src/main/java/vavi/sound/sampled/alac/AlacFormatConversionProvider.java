/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.alac;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;

import com.beatofthedrum.alacdecoder.Alac;


/**
 * AlacFormatConversionProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 111022 nsano initial version <br>
 */
public class AlacFormatConversionProvider extends FormatConversionProvider {

    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        return new AudioFormat.Encoding[] { AlacEncoding.ALAC, AudioFormat.Encoding.PCM_SIGNED };
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        return new AudioFormat.Encoding[] { AlacEncoding.ALAC, AudioFormat.Encoding.PCM_SIGNED };
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
            return new AudioFormat.Encoding[] { AlacEncoding.ALAC };
        } else if (sourceFormat.getEncoding() instanceof AlacEncoding) {
            return new AudioFormat.Encoding[] { AudioFormat.Encoding.PCM_SIGNED };
        } else {
            return new AudioFormat.Encoding[0];
        }
    }

    @Override
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) &&
            targetEncoding instanceof AlacEncoding) {
            if (sourceFormat.getChannels() > 2 ||
                sourceFormat.getChannels() <= 0 ||
                sourceFormat.isBigEndian()) {
                return new AudioFormat[0];
            } else {
                return new AudioFormat[] {
                    new AudioFormat(targetEncoding,
                                    sourceFormat.getSampleRate(),
                                    -1,     // sample size in bits
                                    sourceFormat.getChannels(),
                                    -1,     // frame size
                                    -1,     // frame rate
                                    false)  // little endian
                };
            }
        } else if (sourceFormat.getEncoding() instanceof AlacEncoding && targetEncoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
            return new AudioFormat[] {
                new AudioFormat(sourceFormat.getSampleRate(),
                                16,         // sample size in bits
                                sourceFormat.getChannels(),
                                true,       // signed
                                false)      // little endian (for PCM wav)
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
                    } else if (sourceFormat.getEncoding() instanceof AlacEncoding && targetFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
                        Alac alac = Alac.class.cast(sourceFormat.getProperty("alac"));
                        return new Alac2PcmAudioInputStream(sourceStream, targetFormat, AudioSystem.NOT_SPECIFIED, alac);
                    } else if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) && targetFormat.getEncoding() instanceof AlacEncoding) {
                        throw new IllegalArgumentException("unable to convert " + sourceFormat.toString() + " to " + targetFormat.toString());
                    } else {
                        throw new IllegalArgumentException("unable to convert " + sourceFormat.toString() + " to " + targetFormat.toString());
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
                    } else if (sourceFormat.getEncoding() instanceof AlacEncoding &&
                               targetFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
                        Alac alac = Alac.class.cast(sourceFormat.getProperty("alac"));
                        return new Alac2PcmAudioInputStream(sourceStream, targetFormat, AudioSystem.NOT_SPECIFIED, alac);
                    } else if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) && targetFormat.getEncoding() instanceof AlacEncoding) {
                        throw new IllegalArgumentException("unable to convert " + sourceFormat.toString() + " to " + targetFormat.toString());
                    } else {
                        throw new IllegalArgumentException("unable to convert " + sourceFormat.toString() + " to " + targetFormat.toString());
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
