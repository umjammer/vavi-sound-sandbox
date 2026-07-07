/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.xma;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

import vavi.sound.xma.XmaContainer;
import vavi.sound.xma.XmaStreamInfo;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.TRACE;
import static java.lang.System.getLogger;
import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;


/**
 * Provider for WMA Pro / XMA audio file reading services. This implementation
 * can parse the format information from an XMA (RIFF/WAVE) audio file, and can
 * produce audio input streams from files of this type.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260707 nsano initial version <br>
 */
public class XmaAudioFileReader extends AudioFileReader {

    private static final Logger logger = getLogger(XmaAudioFileReader.class.getName());

    /** how many bytes to sniff to find the RIFF header + fmt chunk */
    private static final int PROBE_SIZE = 0x10000;

    @Override
    public AudioFileFormat getAudioFileFormat(java.io.File file) throws UnsupportedAudioFileException, IOException {
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            return getAudioFileFormat(inputStream, (int) file.length());
        }
    }

    @Override
    public AudioFileFormat getAudioFileFormat(URL url) throws UnsupportedAudioFileException, IOException {
        try (InputStream inputStream = new BufferedInputStream(url.openStream())) {
            return getAudioFileFormat(inputStream);
        }
    }

    @Override
    public AudioFileFormat getAudioFileFormat(InputStream stream) throws UnsupportedAudioFileException, IOException {
        return getAudioFileFormat(stream, NOT_SPECIFIED);
    }

    /**
     * Return the AudioFileFormat from the given InputStream.
     *
     * @param bitStream input to probe (must support mark/reset)
     * @param mediaLength unused
     * @return an AudioFileFormat object based on the audio file data.
     * @throws UnsupportedAudioFileException if the stream is not a valid XMA file.
     * @throws IOException if an I/O exception occurs.
     */
    protected static AudioFileFormat getAudioFileFormat(InputStream bitStream, int mediaLength) throws UnsupportedAudioFileException, IOException {
logger.log(DEBUG, "enter available: " + bitStream.available());
        XmaStreamInfo info;
        try {
            bitStream.mark(PROBE_SIZE);
            byte[] buf = new byte[PROBE_SIZE];
            int n = 0;
            while (n < buf.length) {
                int r = bitStream.read(buf, n, buf.length - n);
                if (r < 0) {
                    break;
                }
                n += r;
            }
            info = XmaContainer.probeInfo(Arrays.copyOf(buf, n));
        } catch (IOException e) {
logger.log(DEBUG, e.toString());
logger.log(TRACE, e.getMessage(), e);
            throw (UnsupportedAudioFileException) new UnsupportedAudioFileException(e.getMessage()).initCause(e);
        } finally {
            bitStream.reset();
logger.log(DEBUG, "finally available: " + bitStream.available());
        }

        AudioFormat format = new AudioFormat(XmaEncoding.XMA,
                info.sampleRate,
                NOT_SPECIFIED,
                info.channels,
                NOT_SPECIFIED,
                NOT_SPECIFIED,
                false,
                new HashMap<>() {{
                    put("xma", info);
                }});
        return new AudioFileFormat(XmaFileFormatType.XMA, format, NOT_SPECIFIED);
    }

    @Override
    public AudioInputStream getAudioInputStream(java.io.File file) throws UnsupportedAudioFileException, IOException {
        InputStream inputStream = new BufferedInputStream(Files.newInputStream(file.toPath()));
        try {
            return getAudioInputStream(inputStream, (int) file.length());
        } catch (UnsupportedAudioFileException | IOException e) {
            inputStream.close();
            throw e;
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException {
        InputStream inputStream = new BufferedInputStream(url.openStream());
        try {
            return getAudioInputStream(inputStream);
        } catch (UnsupportedAudioFileException | IOException e) {
            inputStream.close();
            throw e;
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(InputStream stream) throws UnsupportedAudioFileException, IOException {
        return getAudioInputStream(stream, NOT_SPECIFIED);
    }

    /**
     * Obtains an audio input stream from the input stream provided.
     *
     * @param inputStream the input stream (must support mark/reset)
     * @param mediaLength unused
     * @return an AudioInputStream object based on the audio file data.
     * @throws UnsupportedAudioFileException if the stream is not a valid XMA file.
     * @throws IOException if an I/O exception occurs.
     */
    protected static AudioInputStream getAudioInputStream(InputStream inputStream, int mediaLength) throws UnsupportedAudioFileException, IOException {
        AudioFileFormat audioFileFormat = getAudioFileFormat(inputStream, mediaLength);
        return new AudioInputStream(inputStream, audioFileFormat.getFormat(), audioFileFormat.getFrameLength());
    }
}
