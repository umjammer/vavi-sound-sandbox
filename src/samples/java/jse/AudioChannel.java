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

package jse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;


/**
 * AudioChannel.
 *
 * This file is part of the Java Sound Examples.
 * IDEA: can this class be derived from AudioStream??
 */
public class AudioChannel extends Thread {
    private static final boolean DEBUG = true;
    private static final int BUFFER_SIZE = 16384;
    private List<AudioInputStream> m_audioStreamQueue;
    private SourceDataLine m_line;
    private byte[] m_dataArray;

    /**
     * Uses the passed Mixer.
     */
    public AudioChannel(SourceDataLine line) {
        super("AudioChannel");

        // Setting this thread to daemon means that this thread
        // doesn't prevent the VM from exiting even if it is
        // still running.
        setDaemon(true);

        // TODO: check if priority makes sense
        setPriority(9);
        m_line = line;
        m_audioStreamQueue = new ArrayList<AudioInputStream>();

        // TODO: make size configurable
        int nBufSizeInFrames = 0;
        if (m_line.getBufferSize() > 0) {
            nBufSizeInFrames = m_line.getBufferSize() / 2;
        } else {
            nBufSizeInFrames = 4096;
        }
        m_dataArray = new byte[nBufSizeInFrames];
    }

    public AudioFormat getFormat() {
        return m_line.getFormat();
    }

    public boolean addAudioInputStream(AudioInputStream audioStream) {
        if (DEBUG) {
            System.out.println("AudioChannel.addAudioInputStream(): called.");
        }
        if (!getFormat().matches(audioStream.getFormat())) {
            if (DEBUG) {
                System.out.println("AudioChannel.addAudioInputStream(): audio formats do not match, trying to convert.");
            }

            AudioInputStream asold = audioStream;
            audioStream = AudioSystem.getAudioInputStream(getFormat(), asold);
            if (audioStream == null) {
                System.out.println("###  AudioChannel.addAudioInputStream(): could not convert.");
                return false;
            }
            if (DEBUG) {
                System.out.println(" converted");
            }
        }
        synchronized (m_audioStreamQueue) {
            m_audioStreamQueue.add(audioStream);
            m_audioStreamQueue.notifyAll();
        }
        if (DEBUG) {
            System.out.println("AudioChannel.addAudioInputStream(): enqueued " +
                               audioStream);
        }
        return true;
    }

    // TODO: termination of loop
    public void run() {
        if (DEBUG) {
            System.out.println("AudioChannel.run(): starting");
        }
        while (true) {
            AudioInputStream audioStream = null;
            synchronized (m_audioStreamQueue) {
                while (m_audioStreamQueue.size() == 0) {
                    try {
                        m_audioStreamQueue.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                audioStream = m_audioStreamQueue.remove(0);
            }
            if (DEBUG) {
                System.out.println("AudioChannel.run(): playing " +
                                   audioStream);
            }

            int nBytesRead;
            while (true) {
                try {
                    nBytesRead = audioStream.read(m_dataArray);
                    if (nBytesRead == -1) {
                        // m_line.write(null, 0, 0);
                        break;
                    }

                    int nBytesWritten = m_line.write(m_dataArray, 0, nBytesRead);

                    // Contract.check(nBytesWritten == nBytesRead);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    public void closeChannel() {
        // TODO
    }

    public void startChannel() {
        // TODO
        m_line.start();
        super.start();
    }

    // should not block, but trigger the termination
    public void stopChannel() {
        // TODO do some mystery to
        // a) stop the line (without interupting current plays)
        // b) stop the Thread
    }
}

/* */
