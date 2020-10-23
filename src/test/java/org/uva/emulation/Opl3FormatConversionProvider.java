/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.uva.emulation;

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

    /**
     * Obtains the set of source format encodings from which format conversion
     * services are provided by this provider.
     *
     * @return array of source format encodings. The array will always have a
     *         length of at least 1.
     */
    public AudioFormat.Encoding[] getSourceEncodings() {
        return new AudioFormat.Encoding[] { Opl3Encoding.MID, Opl3Encoding.DRO1, Opl3Encoding.DRO2 };
    }

    /**
     * Obtains the set of target format encodings to which format conversion
     * services are provided by this provider.
     *
     * @return array of target format encodings. The array will always have a
     *         length of at least 1.
     */
    public AudioFormat.Encoding[] getTargetEncodings() {
        return new AudioFormat.Encoding[] { AudioFormat.Encoding.PCM_SIGNED };
    }

    /**
     * Obtains the set of target format encodings supported by the format
     * converter given a particular source format. If no target format encodings
     * are supported for this source format, an array of length 0 is returned.
     *
     * @param sourceFormat format of the incoming data.
     * @return array of supported target format encodings.
     */
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding() instanceof Opl3Encoding) {
            return new AudioFormat.Encoding[] { AudioFormat.Encoding.PCM_SIGNED };
        } else {
            return new AudioFormat.Encoding[0];
        }
    }

    /**
     * Obtains the set of target formats with the encoding specified supported
     * by the format converter. If no target formats with the specified encoding
     * are supported for this source format, an array of length 0 is returned.
     *
     * @param targetEncoding desired encoding of the outgoing data.
     * @param sourceFormat format of the incoming data.
     * @return array of supported target formats.
     */
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding() instanceof Opl3Encoding && targetEncoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
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

    /**
     * Obtains an audio input stream with the specified encoding from the given
     * audio input stream.
     *
     * @param targetEncoding - desired encoding of the stream after processing.
     * @param sourceStream - stream from which data to be processed should be
     *            read.
     * @return stream from which processed data with the specified target
     *         encoding may be read.
     */
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
                        return new Opl3AudioInputStream(sourceStream, targetFormat, AudioSystem.NOT_SPECIFIED, sourceFormat);
                    } else if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) && targetFormat.getEncoding() instanceof Opl3Encoding) {
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

    /**
     * Obtains an audio input stream with the specified format from the given
     * audio input stream.
     *
     * @param targetFormat - desired data format of the stream after processing.
     * @param sourceStream - stream from which data to be processed should be
     *            read.
     * @return stream from which processed data with the specified format may be
     *         read.
     */
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
                        return new Opl3AudioInputStream(sourceStream, targetFormat, AudioSystem.NOT_SPECIFIED, sourceFormat);
                    } else if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) && targetFormat.getEncoding() instanceof Opl3Encoding) {
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
