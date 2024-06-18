/*
 *  Copyright (c) 1999 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
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

package jse;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import static java.lang.System.getLogger;


/**
 * CaptureStream.java
 * <p>
 * This file is part of the Java Sound Examples.
 */
public class CaptureStream extends Thread {

    private static final Logger logger = getLogger(CaptureStream.class.getName());

    private final TargetDataLine m_line;
    private final OutputStream m_outputStream;
    private boolean m_bRecording;

    /**
     * We have to pass:
     * a) an AudioFormat to describe in which format the audio data should be recorded.
     * b) an OutputStream to describe where the recorded data should be written.
     */
    public CaptureStream(AudioFormat format, OutputStream outputStream) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format, AudioSystem.NOT_SPECIFIED);
        m_line = (TargetDataLine) AudioSystem.getLine(info);
        m_line.open(format, m_line.getBufferSize());
        m_outputStream = outputStream;
    }

    @Override
    public void start() {
        m_line.start();
        super.start();
    }

    public void stopRecording() {
        m_line.stop();
        m_line.close();
        m_bRecording = false;
    }

    @Override
    public void run() {
        // TODO: intelligent size
        byte[] abBuffer = new byte[65536];
        int nFrameSize = m_line.getFormat().getFrameSize();
        int nBufferFrames = abBuffer.length / nFrameSize;
        m_bRecording = true;
        while (m_bRecording) {
            logger.log(Level.DEBUG, "Trying to read: " + nBufferFrames);

            int nFramesRead = m_line.read(abBuffer, 0, nBufferFrames);
            logger.log(Level.DEBUG, "Read: " + nFramesRead);

            int nBytesToWrite = nFramesRead * nFrameSize;
            try {
                m_outputStream.write(abBuffer, 0, nBytesToWrite);
            } catch (IOException e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
        }
    }
}
