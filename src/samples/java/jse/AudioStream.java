package jse;
/*
 *        AudioStream.java
 *
 *        This file is part of the Java Sound Examples.
 */
/*
 *  Copyright (c) 1999, 2000 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
 *
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.FloatControl;


/*        IDEA: have a low-level class which only has the separated actions.
        Have a high-level class that commbines them and catches some exceptions.
 */
public class AudioStream implements Runnable {
    /**        Flag for debugging messages.
     *        If true, some messages are dumped to the console
     *        during operation.
     */
    private static boolean DEBUG = true;

    /*
     *        means that the stream has reached EOF or was not started.
     *        This value is returned in property change callbacks that
     *        report the current media position.
     */
    public static final long MEDIA_POSITION_EOF = -1L;
    public static final String MEDIA_POSITION_PROPERTY = "AudioStream_media_position";
    private static final int EXTERNAL_BUFFER_SIZE = 4000 * 4;
    private Thread m_thread = null;
    private Object m_dataSource;
    private AudioInputStream m_audioInputStream;
    private SourceDataLine m_line;
    private FloatControl m_gainControl;
    private FloatControl m_panControl;
    private PropertyChangeSupport m_propertyChangeSupport;

    /**
     *        This variable is used to distinguish stopped state from
     *        paused state. In case of paused state, m_bRunning is still
     *        true. In case of stopped state, it is set to false. Doing so
     *        will terminate the thread.
     */
    private boolean m_bRunning;

    /*
     *        Holds the current media position in frames measured from the
     *        beginning of the stream. This value is reset on a restart of
     *        the stream.
     */
    private long m_lMediaPosition;

    /*
     *        Holds the media position in frames that was the last reported
     *        in a property change callback.
     */
    private long m_lLastReportedMediaPosition;
    private float m_fReportingIntervallInSeconds = 1.0F;

    public AudioStream() {
        m_propertyChangeSupport = new PropertyChangeSupport(this);
        m_dataSource = null;
    }

    public AudioStream(File file)
        throws UnsupportedAudioFileException, LineUnavailableException {
        this();
        setDataSource(file);
    }

    public AudioStream(URL url)
        throws UnsupportedAudioFileException, LineUnavailableException {
        this();
        setDataSource(url);
    }

    public void setDataSource(File file)
        throws UnsupportedAudioFileException, LineUnavailableException {
        m_dataSource = file;
        initAudioInputStream();
    }

    public void setDataSource(URL url)
        throws UnsupportedAudioFileException, LineUnavailableException {
        m_dataSource = url;
        initAudioInputStream();
    }

    private void initAudioInputStream()
        throws UnsupportedAudioFileException, LineUnavailableException {
        if (m_dataSource instanceof URL) {
            initAudioInputStream((URL) m_dataSource);
        } else if (m_dataSource instanceof File) {
            initAudioInputStream((File) m_dataSource);
        }
        initLine();
    }

    private void initAudioInputStream(File file)
        throws UnsupportedAudioFileException {
        try {
            m_audioInputStream = AudioSystem.getAudioInputStream(file);
        } catch (IOException e) {
            throw new IllegalArgumentException("cannot create AudioInputStream for " +
                                               file);
        }
        if (m_audioInputStream == null) {
            throw new IllegalArgumentException("cannot create AudioInputStream for " +
                                               file);
        }
    }

    // from AudioPlayer.java

    /*
     *        Compressed audio data cannot be fed directely to
     *        Java Sound. It has to be converted explicitely.
     *        To do this, we create a new AudioFormat that
     *        says to which format we want to convert to. Then,
     *        we try to get a converted AudioInputStream.
     *        Furthermore, we use the new format and the converted
     *        stream.
     *
     *        Note that the technique shown here is partly non-
     *        portable. It is used here to keep the example
     *        simple. A more advanced, more portable technique
     *        will (hopefully) show up in AudioStream.java soon.
     *
     *        Thanks to Christoph Hecker for finding out that this
     *        was missing.
     */
/*
                if ((audioFormat.getEncoding() == AudioFormat.Encoding.ULAW) ||
                    (audioFormat.getEncoding() == AudioFormat.Encoding.ALAW))
                {
                        if (DEBUG)
                        {
                                System.out.println("AudioPlayer.main(): converting");
                        }
                        AudioFormat newFormat = new AudioFormat(
                                AudioFormat.Encoding.PCM_SIGNED,
                                audioFormat.getSampleRate(),
                                audioFormat.getSampleSizeInBits() * 2,
                                audioFormat.getChannels(),
                                audioFormat.getFrameSize() * 2,
                                audioFormat.getFrameRate(),
                                true);
                        AudioInputStream        newStream = AudioSystem.getAudioInputStream(newFormat, audioInputStream);
                        audioFormat = newFormat;
                        audioInputStream = newStream;
                }
*/
    private void initAudioInputStream(URL url)
        throws UnsupportedAudioFileException {
        try {
            m_audioInputStream = AudioSystem.getAudioInputStream(url);
        }
        // TODO: better pass through?
        catch (IOException e) {
            throw new IllegalArgumentException("cannot create AudioInputStream for " +
                                               url);
        }
        if (m_audioInputStream == null) {
            throw new IllegalArgumentException("cannot create AudioInputStream for " +
                                               url);
        }
    }

    private void initLine() throws LineUnavailableException {
        if (m_line == null) {
            createLine();
            openLine();
        } else {
            AudioFormat lineAudioFormat = m_line.getFormat();
            AudioFormat audioInputStreamFormat = (m_audioInputStream == null)
                                                 ? null
                                                 : m_audioInputStream.getFormat();
            if (!lineAudioFormat.equals(audioInputStreamFormat)) {
                m_line.close();
                openLine();
            }
        }
    }

    private void createLine() throws LineUnavailableException {
        if (m_line != null) {
            return;
        }

        /*
         *        From the AudioInputStream, i.e. from the sound file, we
         *        fetch information about the format of the audio data. These
         *        information include the sampling frequency, the number of
         *        channels and the size of the samples. There information
         *        are needed to ask Java Sound for a suitable output line
         *        for this audio file.
         */
        AudioFormat audioFormat = m_audioInputStream.getFormat();
        if (DEBUG) {
            System.out.println("AudioStream.initLine(): audio format: " +
                               audioFormat);
        }

        /*
         *        Asking for a line is a rather tricky thing.
         *        ...
         *        Furthermore, we have to give Java Sound a hint about how
         *        big the internal buffer for the line should be. Here,
         *        we say AudioSystem.NOT_SPECIFIED, signaling that we don't
         *        care about the exact size. Java Sound will use some default
         *        value for the buffer size.
         */
        DataLine.Info info = new DataLine.Info(SourceDataLine.class,
                                               audioFormat,
                                               AudioSystem.NOT_SPECIFIED);
        m_line = (SourceDataLine) AudioSystem.getLine(info);

        if (m_line.isControlSupported(FloatControl.Type.VOLUME)) {
            m_gainControl = (FloatControl) m_line.getControl(FloatControl.Type.VOLUME);
            if (DEBUG) {
                System.out.println("max gain: " + m_gainControl.getMaximum());
                System.out.println("min gain: " + m_gainControl.getMinimum());
                System.out.println("gain precision: " +
                                   m_gainControl.getPrecision());
            }
        }
        if (m_line.isControlSupported(FloatControl.Type.BALANCE)) {
            m_panControl = (FloatControl) m_line.getControl(FloatControl.Type.BALANCE);
            if (DEBUG) {
                System.out.println("max balance: " + m_panControl.getMaximum());
                System.out.println("min balance: " + m_panControl.getMinimum());
                System.out.println("balance precision: " +
                                   m_panControl.getPrecision());
            }
        }
    }

    private void openLine() throws LineUnavailableException {
        if (m_line == null) {
            return;
        }

        AudioFormat audioFormat = m_audioInputStream.getFormat();
        m_line.open(audioFormat, m_line.getBufferSize());
    }

    // TODO: if class can be instatiated without file or url, m_audioInputStream may
    // be null
    public AudioFormat getFormat() {
        return m_audioInputStream.getFormat();
    }

    public void start() {
        if (DEBUG) {
            System.out.println("start() called");
        }
        if (!((m_thread == null) || !m_thread.isAlive())) {
            if (DEBUG) {
                System.out.println("WARNING: old thread still running!!");
            }
        }
        if (DEBUG) {
            System.out.println("creating new thread");
        }
        m_thread = new Thread(this);
        m_thread.start();
        if (DEBUG) {
            System.out.println("additional thread started");
        }
        if (DEBUG) {
            System.out.println("starting line");
        }
        m_line.start();
    }

    public void stop() {
        if (m_bRunning) {
            if (m_line != null) {
                m_line.stop();
                m_line.flush();
            }
            m_bRunning = false;

            /*
             *        We re-initialize the AudioInputStream. Since doing
             *        a stop on the stream implies that there has been
             *        a successful creation of an AudioInputStream before,
             *        we can almost safely ignore this exception.
             *        The LineUnavailableException can be ignored because
             *        in case of reinitializing the same AudioInputStream,
             *        no new line is created or opened.
             */
            try {
                initAudioInputStream();
            } catch (UnsupportedAudioFileException e) {
            } catch (LineUnavailableException e) {
            }
        }
    }

    public void pause() {
        m_line.stop();
    }

    public void resume() {
        m_line.start();
    }

    public void run() {
        if (DEBUG) {
            System.out.println("thread start");
        }

        long lReportingIntervallInFrames = (long) (m_fReportingIntervallInSeconds * m_line.getFormat()
                                                                                          .getSampleRate() /* * m_line.getFormat().getFrameSize()*/);
        int nBytesRead = 0;
        m_bRunning = true;

        byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];
        int nFrameSize = m_line.getFormat().getFrameSize();
        m_lMediaPosition = 0L;
        m_lLastReportedMediaPosition = 0L;
        fireMediaPositionPropertyChange(MEDIA_POSITION_EOF,
                                        m_lLastReportedMediaPosition);
        while ((nBytesRead != -1) && m_bRunning) {
            try {
                nBytesRead = m_audioInputStream.read(abData, 0, abData.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (nBytesRead >= 0) {
                int nFramesToWrite = nBytesRead / nFrameSize;
                if (DEBUG) {
                    System.out.println("Trying to write: " + nFramesToWrite);
                }

                int nFramesWritten = m_line.write(abData, 0, nFramesToWrite);
                if (DEBUG) {
                    System.out.println("Written: " + nFramesWritten);
                }
                m_lMediaPosition += nFramesWritten;
                if ((m_lMediaPosition - m_lLastReportedMediaPosition) >= lReportingIntervallInFrames) {
                    long lOldReportedMediaPosition = m_lLastReportedMediaPosition;
                    m_lLastReportedMediaPosition += lReportingIntervallInFrames;
                    fireMediaPositionPropertyChange(lOldReportedMediaPosition,
                                                    m_lLastReportedMediaPosition);
                    if (DEBUG) {
                        System.out.println("Position (not always in seconds) = " +
                                           (m_lLastReportedMediaPosition / lReportingIntervallInFrames));
                    }
                }
            }
        }

        /*
         *        Wait until all data are played.
         *        This is only necessary because of the bug noted below.
         *        (If we do not wait, we would interrupt the playback by
         *        prematurely closing the line and exiting the VM.)
         */
        // TODO: check how this interferes with stop()
        m_line.drain();
        if (DEBUG) {
            System.out.println("after drain()");
        }

        /*
         *        Stop the line and reinitialize the AudioInputStream.
         *        This should be done before reporting end-of-media to be
         *        prepared if the EOM message triggers a new start().
         */
        stop();
        if (DEBUG) {
            System.out.println("after this.stop()");
        }
        fireMediaPositionPropertyChange(m_lLastReportedMediaPosition,
                                        MEDIA_POSITION_EOF);
    }

    public boolean hasGainControl() {
        return m_gainControl != null;
    }

/*
        public void setMute(boolean bMute)
        {
                if (hasGainControl())
                {
                        m_gainControl.setMute(bMute);
                }
        }



        public boolean getMute()
        {
                if (hasGainControl())
                {
                        return m_gainControl.getMute();
                }
                else
                {
                        return false;
                }
        }
*/
    public void setGain(float fGain) {
        if (hasGainControl()) {
            m_gainControl.setValue(fGain);
        }
    }

    public float getGain() {
        if (hasGainControl()) {
            return m_gainControl.getValue();
        } else {
            return 0.0F;
        }
    }

    public float getMaximum() {
        if (hasGainControl()) {
            return m_gainControl.getMaximum();
        } else {
            return 0.0F;
        }
    }

    public float getMinimum() {
        if (hasGainControl()) {
            return m_gainControl.getMinimum();
        } else {
            return 0.0F;
        }
    }

    public boolean hasPanControl() {
        return m_panControl != null;
    }

    public float getPrecision() {
        if (hasPanControl()) {
            return m_panControl.getPrecision();
        } else {
            return 0.0F;
        }
    }

    public float getPan() {
        if (hasPanControl()) {
            return m_panControl.getValue();
        } else {
            return 0.0F;
        }
    }

    public void setPan(float fPan) {
        if (hasPanControl()) {
            m_panControl.setValue(fPan);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        m_propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        m_propertyChangeSupport.removePropertyChangeListener(listener);
    }

    private void fireMediaPositionPropertyChange(long lOldPosition,
                                                 long lNewPosition) {
        m_propertyChangeSupport.firePropertyChange(MEDIA_POSITION_PROPERTY,
                                                   new Long(lOldPosition),
                                                   new Long(lNewPosition));
    }
}

/* */
