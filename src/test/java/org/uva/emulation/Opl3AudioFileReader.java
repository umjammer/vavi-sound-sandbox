/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.uva.emulation;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

import vavi.util.Debug;


/**
 * Provider for OPL3 audio file reading services. This implementation can parse
 * the format information from OPL3 audio file, and can produce audio input
 * streams from files of this type.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201022 nsano initial version <br>
 */
public class Opl3AudioFileReader extends AudioFileReader {

    private AudioFileFormat.Type type;
    private AudioFormat.Encoding encoding;

    /** TODO by stream, separate into each player via FileType */
    private void checkExitension(File file) throws UnsupportedAudioFileException {
        type = null;
        encoding = null;
        String ext = file.getName().substring(file.getName().lastIndexOf('.') + 1).toLowerCase();

        if (Opl3FileFormatType.MID.getExtension().contains(ext)) {
            encoding = Opl3Encoding.MID;
            type = Opl3FileFormatType.MID;
        } else if (Opl3FileFormatType.DRO1.getExtension().contains(ext)) {
        } else {
            throw new UnsupportedAudioFileException("unrecognized extension: " + ext);
        }
    }

    @Override
    public AudioFileFormat getAudioFileFormat(File file) throws UnsupportedAudioFileException, IOException {
        checkExitension(file);

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            return getAudioFileFormat(inputStream, (int) file.length());
        } finally {
            inputStream.close();
        }
    }

    @Override
    public AudioFileFormat getAudioFileFormat(URL url) throws UnsupportedAudioFileException, IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AudioFileFormat getAudioFileFormat(InputStream stream) throws UnsupportedAudioFileException, IOException {
        throw new UnsupportedOperationException();
    }

    /** TODO who defined 49700? */
    public static final AudioFormat opl3 = new AudioFormat(49700.0f, 16, 2, true, false);

    /**
     * Return the AudioFileFormat from the given InputStream. Implementation.
     *
     * @param bitStream
     * @param mediaLength
     * @return an AudioInputStream object based on the audio file data contained
     *         in the input stream.
     * @exception UnsupportedAudioFileException if the File does not point to a
     *                valid audio file data recognized by the system.
     * @exception IOException if an I/O exception occurs.
     */
    protected AudioFileFormat getAudioFileFormat(InputStream bitStream, int mediaLength) throws UnsupportedAudioFileException, IOException {
        if (encoding == null) {
            DataInputStream is = new DataInputStream(bitStream);
            try {
                byte[] buf = new byte[10];
                is.mark(10);
                is.readFully(buf);
                if (buf[10] == 1) {
                    encoding = Opl3Encoding.DRO1;
                    type = Opl3FileFormatType.DRO1;
                } else if (buf[8] == 2) {
                    encoding = Opl3Encoding.DRO2;
                    type = Opl3FileFormatType.DRO2;
                }
                is.reset();
                if (encoding == null || type == null) {
                    throw new UnsupportedAudioFileException("bad dro type");
                }
            } finally {
                try {
                    is.reset();
                } catch (IOException e) {
                    Debug.println(Level.FINE, e);
                }
            }
        }
        // specification for around frame might cause AudioInputStream modification at below (*1)
        AudioFormat format = new AudioFormat(encoding, opl3.getSampleRate(), AudioSystem.NOT_SPECIFIED, opl3.getChannels(), AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, opl3.isBigEndian());
        return new AudioFileFormat(type, format, AudioSystem.NOT_SPECIFIED);
    }

    @Override
    public AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
        checkExitension(file);

        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
        try {
            return getAudioInputStream(inputStream, (int) file.length());
        } catch (UnsupportedAudioFileException e) {
            inputStream.close();
            throw e;
        } catch (IOException e) {
            inputStream.close();
            throw e;
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AudioInputStream getAudioInputStream(InputStream stream) throws UnsupportedAudioFileException, IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Obtains an audio input stream from the input stream provided. The stream
     * must point to valid audio file data.
     *
     * @param inputStream the input stream from which the AudioInputStream
     *            should be constructed.
     * @param medialength
     * @return an AudioInputStream object based on the audio file data contained
     *         in the input stream.
     * @exception UnsupportedAudioFileException if the File does not point to a
     *                valid audio file data recognized by the system.
     * @exception IOException if an I/O exception occurs.
     */
    protected AudioInputStream getAudioInputStream(InputStream inputStream, int medialength) throws UnsupportedAudioFileException, IOException {
        AudioFileFormat audioFileFormat = getAudioFileFormat(inputStream, medialength);
        // (*1) audioFileFormat should not be detailed about frame size
        return new AudioInputStream(inputStream, audioFileFormat.getFormat(), audioFileFormat.getFrameLength());
    }
}
