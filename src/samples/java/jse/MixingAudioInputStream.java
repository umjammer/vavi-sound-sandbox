/*
 * Copyright (c) 1999 - 2001 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
 *
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as published
 * by the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package jse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.tritonus.share.sampled.TConversionTool;

import static java.lang.System.getLogger;


/*
 * MixingAudioInputStream.java
 *
 * This file is part of the Java Sound Examples.
 *
 * This code follows an idea of Paul Sorenson.
 *
 * Mixing of multiple AudioInputStreams to one AudioInputStream. This class
 * takes a collection of AudioInputStreams and mixes them together. Being a
 * subclass of AudioInputStream itself, reading from instances of this class
 * behaves as if the mixdown result of the input streams is read.
 *
 * @author Matthias Pfisterer
 */
public class MixingAudioInputStream extends AudioInputStream {

    private static final Logger logger = getLogger(MixingAudioInputStream.class.getName());

    private final List<AudioInputStream> m_audioInputStreamList;

    public MixingAudioInputStream(AudioFormat audioFormat, Collection<AudioInputStream> audioInputStreams) {
        super(new ByteArrayInputStream(new byte[0]), audioFormat, AudioSystem.NOT_SPECIFIED);
        logger.log(Level.DEBUG, "MixingAudioInputStream.<init>(): begin");
        m_audioInputStreamList = new ArrayList<>(audioInputStreams);
        if (logger.isLoggable(Level.DEBUG)) {
            logger.log(Level.DEBUG, "MixingAudioInputStream.<init>(): stream list:");
            for (AudioInputStream audioInputStream : m_audioInputStreamList) {
                logger.log(Level.DEBUG, "  " + audioInputStream);
            }
        }
        logger.log(Level.DEBUG, "MixingAudioInputStream.<init>(): end");
    }

    // TODO remove
    private boolean addAudioInputStream(AudioInputStream audioStream) {
        logger.log(Level.DEBUG, "MixingAudioInputStream.addAudioInputStream(): called.");

        // Contract.check(audioStream != null);
        if (!getFormat().matches(audioStream.getFormat())) {
            logger.log(Level.DEBUG, "MixingAudioInputStream.addAudioInputStream(): audio formats do not match, trying to convert.");

            AudioInputStream asold = audioStream;
            audioStream = AudioSystem.getAudioInputStream(getFormat(), asold);
            if (audioStream == null) {
                logger.log(Level.DEBUG, "###  MixingAudioInputStream.addAudioInputStream(): could not convert.");
                return false;
            }
            logger.log(Level.DEBUG, " converted");
        }

        // Contract.check(audioStream != null);
        synchronized (m_audioInputStreamList) {
            m_audioInputStreamList.add(audioStream);
            m_audioInputStreamList.notifyAll();
        }
        logger.log(Level.DEBUG, "MixingAudioInputStream.addAudioInputStream(): enqueued " + audioStream);
        return true;
    }

    /**
     * The maximum of the frame length of the input stream is calculated and
     * returned. If at least one of the input streams has length
     * <code>AudioInputStream.NOT_SPECIFIED</code>, this value is returned.
     */
    @Override
    public long getFrameLength() {
        long lLengthInFrames = 0;
        for (AudioInputStream stream : m_audioInputStreamList) {
            long lLength = stream.getFrameLength();
            if (lLength == AudioSystem.NOT_SPECIFIED) {
                return AudioSystem.NOT_SPECIFIED;
            } else {
                lLengthInFrames = Math.max(lLengthInFrames, lLength);
            }
        }
        return lLengthInFrames;
    }

    @Override
    public int read() throws IOException {
        logger.log(Level.DEBUG, "MixingAudioInputStream.read(): begin");

        int nSample = 0;
        Iterator<AudioInputStream> streamIterator = m_audioInputStreamList.iterator();
        while (streamIterator.hasNext()) {
            AudioInputStream stream = streamIterator.next();
            int nByte = stream.read();
            if (nByte == -1) {
                // The end of this stream has been signaled. We remove the
                // stream from our list.
                streamIterator.remove();
                continue;
            } else {
                // what about signed/unsigned?
                nSample += nByte;
            }
        }
        logger.log(Level.DEBUG, "MixingAudioInputStream.read(): end");
        return (byte) nSample;
    }

    @Override
    public int read(byte[] abData, int nOffset, int nLength) throws IOException {
        logger.log(Level.DEBUG, "MixingAudioInputStream.read(byte[], int, int): begin");
        logger.log(Level.DEBUG, "MixingAudioInputStream.read(byte[], int, int): requested length: " + nLength);

        int nChannels = getFormat().getChannels();
        int nFrameSize = getFormat().getFrameSize();

        // This value is in bytes. Note that it is the storage size. It may be
        // four bytes for 24 bit samples.
        int nSampleSize = nFrameSize / nChannels;
        boolean bBigEndian = getFormat().isBigEndian();
        AudioFormat.Encoding encoding = getFormat().getEncoding();
        logger.log(Level.DEBUG, "MixingAudioInputStream.read(byte[], int, int): channels: " + nChannels);
        logger.log(Level.DEBUG, "MixingAudioInputStream.read(byte[], int, int): frame size: " + nFrameSize);
        logger.log(Level.DEBUG, "MixingAudioInputStream.read(byte[], int, int): sample size (bytes, storage size): " + nSampleSize);
        logger.log(Level.DEBUG, "MixingAudioInputStream.read(byte[], int, int): big endian: " + bBigEndian);
        logger.log(Level.DEBUG, "MixingAudioInputStream.read(byte[], int, int): encoding: " + encoding);

        byte[] abBuffer = new byte[nFrameSize];
        int[] anMixedSamples = new int[nChannels];
        for (int nFrameBoundry = 0; nFrameBoundry < nLength; nFrameBoundry += nFrameSize) {
            logger.log(Level.DEBUG, "MixingAudioInputStream.read(byte[], int, int): frame boundry: " + nFrameBoundry);
            Arrays.fill(anMixedSamples, 0);

            Iterator<AudioInputStream> streamIterator = m_audioInputStreamList.iterator();
            while (streamIterator.hasNext()) {
                AudioInputStream stream = streamIterator.next();
                logger.log(Level.DEBUG, "MixingAudioInputStream.read(byte[], int, int): AudioInputStream: " + stream);

                int nBytesRead = stream.read(abBuffer, 0, nFrameSize);
                logger.log(Level.DEBUG, "MixingAudioInputStream.read(byte[], int, int): bytes read: " + nBytesRead);

                // TODO: we have to handle incomplete reads.
                if (nBytesRead == -1) {
                    // The end of the current stream has been signaled. We
                    // remove it from the list of streams.
                    streamIterator.remove();
                    continue;
                }
                for (int nChannel = 0; nChannel < nChannels; nChannel++) {
                    int nBufferOffset = nChannel * nSampleSize;
                    int nSampleToAdd = 0;
                    if (encoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
                        nSampleToAdd = switch (nSampleSize) {
                            case 1 -> abBuffer[nBufferOffset];
                            case 2 -> TConversionTool.bytesToInt16(abBuffer, nBufferOffset, bBigEndian);
                            case 3 -> TConversionTool.bytesToInt24(abBuffer, nBufferOffset, bBigEndian);
                            case 4 -> TConversionTool.bytesToInt32(abBuffer, nBufferOffset, bBigEndian);
                            default -> nSampleToAdd;
                        };
                    }
                    // TODO: pcm unsigned
                    else if (encoding.equals(AudioFormat.Encoding.ALAW)) {
                        nSampleToAdd = TConversionTool.alaw2linear(abBuffer[nBufferOffset]);
                    } else if (encoding.equals(AudioFormat.Encoding.ULAW)) {
                        nSampleToAdd = TConversionTool.ulaw2linear(abBuffer[nBufferOffset]);
                    }
                    anMixedSamples[nChannel] += nSampleToAdd;
                }
            }
            logger.log(Level.DEBUG, "MixingAudioInputStream.read(byte[], int, int): starting to write to buffer passed by caller");
            for (int nChannel = 0; nChannel < nChannels; nChannel++) {
                logger.log(Level.DEBUG, "MixingAudioInputStream.read(byte[], int, int): channel: " + nChannel);

                int nBufferOffset = nOffset + nFrameBoundry /* * nFrameSize */ + (nChannel * nSampleSize);
                logger.log(Level.DEBUG, "MixingAudioInputStream.read(byte[], int, int): buffer offset: " + nBufferOffset);
                if (encoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
                    switch (nSampleSize) {
                        case 1:
                            abData[nBufferOffset] = (byte) anMixedSamples[nChannel];
                            break;
                        case 2:
                            TConversionTool.intToBytes16(anMixedSamples[nChannel], abData, nBufferOffset, bBigEndian);
                            break;
                        case 3:
                            TConversionTool.intToBytes24(anMixedSamples[nChannel], abData, nBufferOffset, bBigEndian);
                            break;
                        case 4:
                            TConversionTool.intToBytes32(anMixedSamples[nChannel], abData, nBufferOffset, bBigEndian);
                            break;
                    }
                }
                // TODO: pcm unsigned
                else if (encoding.equals(AudioFormat.Encoding.ALAW)) {
                    abData[nBufferOffset] = TConversionTool.linear2alaw((short) anMixedSamples[nChannel]);
                } else if (encoding.equals(AudioFormat.Encoding.ULAW)) {
                    abData[nBufferOffset] = TConversionTool.linear2ulaw(anMixedSamples[nChannel]);
                }
            }
        }
        logger.log(Level.DEBUG, "MixingAudioInputStream.read(byte[], int, int): end");

        // TODO: return a useful value
        return nLength;
    }

    /**
     * calls skip() on all input streams. There is no way to assure that the
     * number of bytes really skipped is the same for all input streams. Due to
     * that, this method always returns the passed value. In other words: the
     * return value is useless (better ideas appreciated).
     */
    @Override
    public long skip(long lLength) throws IOException {
//      int nAvailable = 0;
        for (AudioInputStream stream : m_audioInputStreamList) {
            stream.skip(lLength);
        }
        return lLength;
    }

    /**
     * The minimum of available() of all input stream is calculated and
     * returned.
     */
    @Override
    public int available() throws IOException {
        int nAvailable = 0;
        for (AudioInputStream stream : m_audioInputStreamList) {
            nAvailable = Math.min(nAvailable, stream.available());
        }
        return nAvailable;
    }

    @Override
    public void close() throws IOException {
        // TODO: should we close all streams in the list?
    }

    @Override
    public void mark(int nReadLimit) {
        for (AudioInputStream stream : m_audioInputStreamList) {
            stream.mark(nReadLimit);
        }
    }

    @Override
    public void reset() throws IOException {
        for (AudioInputStream stream : m_audioInputStreamList) {
            stream.reset();
        }
    }

    @Override
    public boolean markSupported() {
        for (AudioInputStream stream : m_audioInputStreamList) {
            if (!stream.markSupported()) {
                return false;
            }
        }
        return true;
    }
}
