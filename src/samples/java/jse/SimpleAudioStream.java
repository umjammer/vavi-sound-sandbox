package jse;
/*
 *        SimpleAudioStream.java
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;


public class SimpleAudioStream extends BaseAudioStream {

//    private static final int EXTERNAL_BUFFER_SIZE = 16384;

    /**
     * This variable is used to distinguish stopped state from
     * paused state. In case of paused state, m_bRunning is still
     * true. In case of stopped state, it is set to false. Doing so
     * will terminate the thread.
     */
    private boolean m_bRunning;

    public SimpleAudioStream() {
        super();

//        m_dataSource = null;
    }

    public SimpleAudioStream(File file)
            throws UnsupportedAudioFileException, LineUnavailableException, IOException {
        this();
        setDataSource(file);
        initLine();
    }

    public SimpleAudioStream(URL url)
            throws UnsupportedAudioFileException, LineUnavailableException, IOException {
        this();
        setDataSource(url);
        initLine();
    }

    @Override
    public AudioFormat getFormat() {
        // TODO: have to check that AudioInputStream (or Line?) is initialized
        return super.getFormat();
    }

//    public void start() {
//        logger.log(Level.TRACE, "start() called");
//        m_thread = new Thread(this);
//        m_thread.start();
//        logger.log(Level.TRACE, "additional thread started");
//        m_line.start();
//        logger.log(Level.TRACE, "started line");
//    }
//
//    public void stop() {
//        m_line.stop();
//        m_line.flush();
//        m_bRunning = false;
//    }
//
//    public void pause() {
//        m_line.stop();
//    }
//
//    public void resume() {
//        m_line.start();
//    }
//
//    public void run() {
//        logger.log(Level.TRACE, "thread start");
//        int nBytesRead = 0;
//        m_bRunning = true;
//        byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];
//        int nFrameSize = m_line.getFormat().getFrameSize();
//        while (nBytesRead != -1 && m_bRunning) {
//            try {
//                nBytesRead = m_audioInputStream.read(abData, 0, abData.length);
//            } catch (IOException e) {
//                logger.log(Level.ERROR, e.getMessage(), e);
//            }
//            if (nBytesRead >= 0) {
//                int nRemainingBytes = nBytesRead;
////                        while (nRemainingBytes > 0)
////                {
//                logger.log(Level.ERROR, "Line status (active): " + m_line.isActive());
//                logger.log(Level.ERROR, "Line status (running): " + m_line.isRunning());
//                logger.log(Level.ERROR, "Trying to write (bytes): " + nBytesRead);
//                int nBytesWritten = m_line.write(abData, 0, nBytesRead);
//                logger.log(Level.ERROR, "Written (bytes): " + nBytesWritten);
//                nRemainingBytes -= nBytesWritten;
////                }
//            }
//        }
//        logger.log(Level.TRACE, "after main loop");
//    }
}
