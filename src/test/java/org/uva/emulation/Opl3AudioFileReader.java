/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.uva.emulation;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;


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

    /**
     * Obtains the audio file format of the File provided. The File must point
     * to valid audio file data.
     *
     * @param file the File from which file format information should be
     *            extracted.
     * @return an AudioFileFormat object describing the audio file format.
     * @exception UnsupportedAudioFileException if the File does not point to a
     *                valid audio file data recognized by the system.
     * @exception IOException if an I/O exception occurs.
     */
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

    /**
     * Obtains an audio input stream from the URL provided. The URL must point
     * to valid audio file data.
     *
     * @param url the URL for which the AudioInputStream should be constructed.
     * @return an AudioInputStream object based on the audio file data pointed
     *         to by the URL.
     * @exception UnsupportedAudioFileException if the File does not point to a
     *                valid audio file data recognized by the system.
     * @exception IOException if an I/O exception occurs.
     */
    public AudioFileFormat getAudioFileFormat(URL url) throws UnsupportedAudioFileException, IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Obtains an audio input stream from the input stream provided.
     *
     * @param stream the input stream from which the AudioInputStream should be
     *            constructed.
     * @return an AudioInputStream object based on the audio file data contained
     *         in the input stream.
     * @exception UnsupportedAudioFileException if the File does not point to a
     *                valid audio file data recognized by the system.
     * @exception IOException if an I/O exception occurs.
     */
    public AudioFileFormat getAudioFileFormat(InputStream stream) throws UnsupportedAudioFileException, IOException {
        throw new UnsupportedOperationException();
    }

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
        }
        AudioFormat format = new AudioFormat(encoding, opl3.getSampleRate(), opl3.getSampleSizeInBits(), opl3.getChannels(), opl3.getFrameSize(), opl3.getFrameRate(), opl3.isBigEndian());
        return new AudioFileFormat(type, format, mediaLength);
    }

    /**
     * Obtains an audio input stream from the File provided. The File must point
     * to valid audio file data.
     *
     * @param file the File for which the AudioInputStream should be
     *            constructed.
     * @return an AudioInputStream object based on the audio file data pointed
     *         to by the File.
     * @exception UnsupportedAudioFileException if the File does not point to a
     *                valid audio file data recognized by the system.
     * @exception IOException if an I/O exception occurs.
     */
    public AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
        checkExitension(file);

        InputStream inputStream = new FileInputStream(file);
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

    /**
     * Obtains an audio input stream from the URL provided. The URL must point
     * to valid audio file data.
     *
     * @param url the URL for which the AudioInputStream should be constructed.
     * @return an AudioInputStream object based on the audio file data pointed
     *         to by the URL.
     * @exception UnsupportedAudioFileException if the File does not point to a
     *                valid audio file data recognized by the system.
     * @exception IOException if an I/O exception occurs.
     */
    public AudioInputStream getAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Obtains an audio input stream from the input stream provided. The stream
     * must point to valid audio file data.
     *
     * @param stream the input stream from which the AudioInputStream should be
     *            constructed.
     * @return an AudioInputStream object based on the audio file data contained
     *         in the input stream.
     * @exception UnsupportedAudioFileException if the File does not point to a
     *                valid audio file data recognized by the system.
     * @exception IOException if an I/O exception occurs.
     */
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
        return new Opl3AudioInputStream(inputStream, opl3, audioFileFormat.getFrameLength(), audioFileFormat.getFormat());
    }
}
