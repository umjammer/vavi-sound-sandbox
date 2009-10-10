package jse;
/*
 *        DirectRecordingStream.java
 *
 *        This file is part of the Java Sound Examples.
 */
/*
 *  Copyright (c) 1999, 2000 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
 *
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */
import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.AudioFileFormat;


/*
  TODO:
 */
public class DirectRecordingStream extends Thread {
    private TargetDataLine m_line;
    private AudioFileFormat.Type m_targetType;
    private AudioInputStream m_audioInputStream;
    private Object m_outputObject;
    private boolean m_bRecording;

    public DirectRecordingStream(AudioFormat audioFormat,
                                 AudioFileFormat.Type targetType,
                                 OutputStream outputStream)
        throws LineUnavailableException {
        this(audioFormat, targetType, (Object) outputStream);
    }

    public DirectRecordingStream(AudioFormat audioFormat,
                                 AudioFileFormat.Type targetType, File file)
        throws LineUnavailableException {
        this(audioFormat, targetType, (Object) file);
    }

    private DirectRecordingStream(AudioFormat audioFormat,
                                  AudioFileFormat.Type targetType,
                                  Object destination)
        throws LineUnavailableException {
        TargetDataLine line = getTargetDataLine(audioFormat);
        init(line, targetType, destination);
    }

    public DirectRecordingStream(TargetDataLine line,
                                 AudioFileFormat.Type targetType,
                                 OutputStream outputStream) {
        this(line, targetType, (Object) outputStream);
    }

    public DirectRecordingStream(TargetDataLine line,
                                 AudioFileFormat.Type targetType, File file) {
        this(line, targetType, (Object) file);
    }

    private DirectRecordingStream(TargetDataLine line,
                                  AudioFileFormat.Type targetType,
                                  Object destination) {
        init(line, targetType, destination);
    }

    private static TargetDataLine getTargetDataLine(AudioFormat audioFormat)
        throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
        TargetDataLine line = null;
        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(audioFormat); // can use open() version?
        return line;
    }

    private void init(TargetDataLine line, AudioFileFormat.Type targetType,
                      Object destination) {
        m_line = line;
        m_audioInputStream = new AudioInputStream(line);
        m_targetType = targetType;
        m_outputObject = destination;
    }

    /**        Starts the recording.
     *        To accomplish this, (i) the line is started and (ii) the
     *        thread is started.
     */
    public void start() {
        m_line.start();
        super.start();
    }

    public void stopRecording() {
        m_line.stop();
        m_line.close();
        m_bRecording = false;
    }

    public void run() {
        if (m_outputObject instanceof File) {
            try {
                AudioSystem.write(m_audioInputStream, m_targetType,
                                  (File) m_outputObject);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (m_outputObject instanceof OutputStream) {
            try {
                AudioSystem.write(m_audioInputStream, m_targetType,
                                  (OutputStream) m_outputObject);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // TODO: error
        }
    }
}

/* */
