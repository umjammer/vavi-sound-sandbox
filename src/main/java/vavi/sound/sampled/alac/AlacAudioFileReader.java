/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.alac;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

import com.beatofthedrum.alacdecoder.Alac;

import vavi.util.Debug;


/**
 * Provider for ALAC audio file reading services. This implementation can parse
 * the format information from ALAC audio file, and can produce audio input
 * streams from files of this type.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 111022 nsano initial version <br>
 */
public class AlacAudioFileReader extends AudioFileReader {

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
        InputStream inputStream = url.openStream();
        try {
            return getAudioFileFormat(inputStream);
        } finally {
            inputStream.close();
        }
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
        return getAudioFileFormat(stream, AudioSystem.NOT_SPECIFIED);
    }

    /**
     * Return the AudioFileFormat from the given InputStream.
     *
     * @param stream the input stream from which the AudioInputStream should be
     *            constructed.
     * @param medialength
     * @return an AudioInputStream object based on the audio file data contained
     *         in the input stream.
     * @exception UnsupportedAudioFileException if the File does not point to a
     *                valid audio file data recognized by the system.
     * @exception IOException if an I/O exception occurs.
     */

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
//Debug.println("here: " + bitStream.markSupported());
        Alac alac;
        try {
            alac = new Alac(bitStream);
        } catch (IOException e) {
Debug.println(e.getMessage());
            throw (UnsupportedAudioFileException) new UnsupportedAudioFileException(e.getMessage()).initCause(e);
        }
        AudioFormat format = new AudioFormat(AlacEncoding.ALAC, alac.getSampleRate(), alac.getBitsPerSample(), alac.getNumChannels(), AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, true, new HashMap<String, Object>() {{ put("alac", alac); }});
        return new AudioFileFormat(AlacFileFormatType.ALAC, format, AudioSystem.NOT_SPECIFIED);
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
        InputStream inputStream = url.openStream();
        try {
            return getAudioInputStream(inputStream);
        } catch (UnsupportedAudioFileException e) {
            inputStream.close();
            throw e;
        } catch (IOException e) {
            inputStream.close();
            throw e;
        }
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
        return getAudioInputStream(stream, AudioSystem.NOT_SPECIFIED);
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
        Alac alac = Alac.class.cast(audioFileFormat.getFormat().getProperty("alac"));
        return new Alac2PcmAudioInputStream(inputStream, audioFileFormat.getFormat(), audioFileFormat.getFrameLength(), alac);
    }
}
