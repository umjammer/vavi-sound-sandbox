/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.wma;

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

import vavi.sound.wma.AsfDemuxer;
import vavi.sound.wma.AsfInfo;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.TRACE;
import static java.lang.System.getLogger;
import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;


/**
 * Provider for WMA v1/v2 (ASF) audio file reading services. Parses the audio
 * stream properties from an ASF ({@code .wma}) container and produces audio
 * input streams for such files.
 * <p>
 * Only WMA v1 (0x0160) and v2 (0x0161) are supported; WMA Pro (0x0162) and
 * Lossless (0x0163) are rejected (XMA/WMA Pro is handled by
 * {@code vavi.sound.sampled.xma} instead).
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260707 nsano initial version <br>
 */
public class WmaAudioFileReader extends AudioFileReader {

    private static final Logger logger = getLogger(WmaAudioFileReader.class.getName());

    /** how many bytes to sniff to find the ASF header and stream properties */
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
     * @throws UnsupportedAudioFileException if the stream is not a supported WMA/ASF file.
     * @throws IOException if an I/O exception occurs.
     */
    protected static AudioFileFormat getAudioFileFormat(InputStream bitStream, int mediaLength) throws UnsupportedAudioFileException, IOException {
logger.log(DEBUG, "enter available: " + bitStream.available());
        AsfInfo info;
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
            byte[] head = Arrays.copyOf(buf, n);
            if (!AsfDemuxer.isAsf(head)) {
                throw new UnsupportedAudioFileException("not an ASF/WMA file");
            }
            info = AsfDemuxer.demux(head);
            if (info.formatTag != 0x0160 && info.formatTag != 0x0161) {
                throw new UnsupportedAudioFileException(
                        String.format("unsupported WMA format tag: 0x%x", info.formatTag));
            }
        } catch (UnsupportedAudioFileException e) {
            throw e;
        } catch (RuntimeException | IOException e) {
logger.log(DEBUG, e.toString());
logger.log(TRACE, e.getMessage(), e);
            throw (UnsupportedAudioFileException) new UnsupportedAudioFileException(e.getMessage()).initCause(e);
        } finally {
            bitStream.reset();
logger.log(DEBUG, "finally available: " + bitStream.available());
        }

        AudioFormat format = new AudioFormat(WmaEncoding.WMA,
                info.sampleRate,
                NOT_SPECIFIED,
                info.channels,
                NOT_SPECIFIED,
                NOT_SPECIFIED,
                false,
                new HashMap<>() {{
                    put("wma", info);
                }});
        return new AudioFileFormat(WmaFileFormatType.WMA, format, NOT_SPECIFIED);
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
     * @throws UnsupportedAudioFileException if the stream is not a supported WMA/ASF file.
     * @throws IOException if an I/O exception occurs.
     */
    protected static AudioInputStream getAudioInputStream(InputStream inputStream, int mediaLength) throws UnsupportedAudioFileException, IOException {
        AudioFileFormat audioFileFormat = getAudioFileFormat(inputStream, mediaLength);
        return new AudioInputStream(inputStream, audioFileFormat.getFormat(), audioFileFormat.getFrameLength());
    }
}
