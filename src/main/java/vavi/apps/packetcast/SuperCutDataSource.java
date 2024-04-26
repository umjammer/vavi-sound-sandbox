/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.packetcast;

import java.io.IOException;

import javax.media.Buffer;
import javax.media.Control;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.Processor;
import javax.media.Time;
import javax.media.control.TrackControl;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;


/**
 * The customed DataSource to cut input.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060127 nsano initial version <br>
 */
public class SuperCutDataSource extends PushBufferDataSource {

    Processor p;

    MediaLocator ml;

    PushBufferDataSource ds;

    SuperCutStream[] streams;

    /**
     * Creates special data source.
     * @param p the processor
     * @param ml source MediaLocator
     * @param start in nano second
     * @param end in nano second
     */
    public SuperCutDataSource(Processor p, MediaLocator ml, long[] start, long[] end) {
        this.p = p;
        this.ml = ml;
        this.ds = (PushBufferDataSource) p.getDataOutput();

        TrackControl[] tcs = p.getTrackControls();
        PushBufferStream[] pbs = ds.getStreams();

        streams = new SuperCutStream[pbs.length];
        for (int i = 0; i < pbs.length; i++) {
            streams[i] = new SuperCutStream(tcs[i], pbs[i], start, end);
        }
    }

    @Override
    public void connect() throws java.io.IOException {
    }

    @Override
    public PushBufferStream[] getStreams() {
        return streams;
    }

    @Override
    public void start() throws java.io.IOException {
        p.start();
        ds.start();
    }

    @Override
    public void stop() throws java.io.IOException {
    }

    @Override
    public Object getControl(String name) {
        // No controls
        return null;
    }

    @Override
    public Object[] getControls() {
        // No controls
        return new Control[0];
    }

    @Override
    public Time getDuration() {
        return ds.getDuration();
    }

    @Override
    public void disconnect() {
    }

    @Override
    public String getContentType() {
        return ContentDescriptor.RAW;
    }

    @Override
    public MediaLocator getLocator() {
        return ml;
    }

    @Override
    public void setLocator(MediaLocator ml) {
        System.err.println("Not interested in a media locator");
    }

    /**
     * Utility Source stream for the SuperCutDataSource.
     */
    static class SuperCutStream implements PushBufferStream, BufferTransferHandler {

        TrackControl tc;

        PushBufferStream pbs;

        long[] start, end;

        boolean[] startReached, endReached;

        int idx = 0;

        BufferTransferHandler bth;

        long timeStamp = 0;

        long lastTS = 0;

        int audioLen = 0;

        int audioElapsed = 0;

        boolean eos = false;

        Format format;

        // Single buffer Queue.
        final Buffer buffer;

        int bufferFilled = 0;

        public SuperCutStream(TrackControl tc, PushBufferStream pbs, long[] start, long[] end) {
            this.tc = tc;
            this.pbs = pbs;
            this.start = start;
            this.end = end;
            startReached = new boolean[start.length];
            endReached = new boolean[end.length];
            for (int i = 0; i < start.length; i++) {
                startReached[i] = endReached[i] = false;
            }
            buffer = new Buffer();
            pbs.setTransferHandler(this);
        }

        /**
         * Called from the transferData to read data from the input.
         */
        void processData() {

            // We have a synchronized buffer Q of 1.
            synchronized (buffer) {
                while (bufferFilled == 1) {
                    try {
                        buffer.wait();
                    } catch (Exception e) {
                    }
                }
            }

            // Read from the real source.
            try {
                pbs.read(buffer);
            } catch (IOException e) {
            }

            format = buffer.getFormat();

            if (idx >= end.length) {
                // We are done with all the end points.
                // Let's just generate an EOM to stop the processing.
                buffer.setOffset(0);
                buffer.setLength(0);
                buffer.setEOM(true);
            }

            if (buffer.isEOM())
                eos = true;

            int len = buffer.getLength();

            // Skip the buffers if it's to be cut.
            if (checkTimeToSkip(buffer)) {
                // Update the audio len counter.
                if (isRawAudio(buffer.getFormat()))
                    audioLen += len;
                return;
            }

            // Update the audio len counter.
            if (isRawAudio(buffer.getFormat()))
                audioLen += len;

            // We can now allow the processor to read from our stream.
            synchronized (buffer) {
                bufferFilled = 1;
                buffer.notifyAll();
            }

            // Notify the processor.
            if (bth != null)
                bth.transferData(this);
        }

        /**
         * This is invoked from the consumer processor to read a frame from me.
         */
        @Override
        public void read(Buffer rdBuf) throws IOException {

            /*
              Check if there's any buffer in the Q to read.
             */
            synchronized (buffer) {
                while (bufferFilled == 0) {
                    try {
                        buffer.wait();
                    } catch (Exception e) {
                    }
                }
            }

            // Copy the data from the queue.
            Object oldData = rdBuf.getData();

            rdBuf.copy(buffer);
            buffer.setData(oldData);

            // Remap the time stamps.

            if (isRawAudio(rdBuf.getFormat())) {
                // Raw audio has a accurate to compute time.
                rdBuf.setTimeStamp(computeDuration(audioElapsed, rdBuf.getFormat()));
                audioElapsed += buffer.getLength();
            } else if (rdBuf.getTimeStamp() != Buffer.TIME_UNKNOWN) {
                long diff = rdBuf.getTimeStamp() - lastTS;
                lastTS = rdBuf.getTimeStamp();
                if (diff > 0)
                    timeStamp += diff;
                rdBuf.setTimeStamp(timeStamp);
            }

            synchronized (buffer) {
                bufferFilled = 0;
                buffer.notifyAll();
            }
        }

        /**
         * Given a buffer, check to see if this should be included or skipped based
         * on the start and end times.
         */
        boolean checkTimeToSkip(Buffer buf) {

            if (idx >= startReached.length)
                return false;

            if (!eos && !startReached[idx]) {
                if (!(startReached[idx] = checkStartTime(buf, start[idx]))) {
                    return true;
                }
            }

            if (!eos && !endReached[idx]) {
                //noinspection AssignmentUsedAsCondition
                if (endReached[idx] = checkEndTime(buf, end[idx])) {
                    idx++; // move on to the next set of start & end pts.
                    return true;
                }
            } else if (endReached[idx]) {
                if (!eos) {
                    return true;
                } else {
                    buf.setOffset(0);
                    buf.setLength(0);
                }
            }

            return false;
        }

        /**
         * Check the buffer against the start time.
         */
        boolean checkStartTime(Buffer buf, long startTS) {
            if (isRawAudio(buf.getFormat())) {
                long ts = computeDuration(audioLen + buf.getLength(), buf.getFormat());
                if (ts > startTS) {
                    int len = computeLength(ts - startTS, buf.getFormat());
                    buf.setOffset(buf.getOffset() + buf.getLength() - len);
                    buf.setLength(len);
                    lastTS = buf.getTimeStamp();
                    return true;
                }
            } else if (buf.getTimeStamp() >= startTS) {
                if (buf.getFormat() instanceof VideoFormat) {
                    // The starting frame needs to be a key frame.
                    if ((buf.getFlags() & Buffer.FLAG_KEY_FRAME) != 0) {
                        lastTS = buf.getTimeStamp();
                        return true;
                    }
                } else {
                    lastTS = buf.getTimeStamp();
                    return true;
                }
            }
            return false;
        }

        /**
         * Check the buffer against the end time.
         */
        boolean checkEndTime(Buffer buf, long endTS) {
            if (isRawAudio(buf.getFormat())) {
                if (computeDuration(audioLen, buf.getFormat()) >= endTS)
                    return true;
                else {
                    long ts = computeDuration(audioLen + buf.getLength(), buf.getFormat());
                    if (ts >= endTS) {
                        int len = computeLength(ts - endTS, buf.getFormat());
                        buf.setLength(buf.getLength() - len);
                        // We still need to process this last buffer.
                    }
                }
            } else if (buf.getTimeStamp() > endTS) {
                return true;
            }

            return false;
        }

        /**
         * Compute the duration based on the length and format of the audio.
         */
        public static long computeDuration(int len, Format fmt) {
            if (!(fmt instanceof AudioFormat))
                return -1;
            return ((AudioFormat) fmt).computeDuration(len);
        }

        /**
         * Compute the length based on the duration and format of the audio.
         */
        public static int computeLength(long duration, Format fmt) {
            if (!(fmt instanceof AudioFormat af))
                return -1;
            // Multiplication is done is stages to avoid overflow.
            return (int) ((((duration / 1000) * (af.getChannels() * af.getSampleSizeInBits())) / 1000) * af.getSampleRate() / 8000);
        }

        @Override
        public ContentDescriptor getContentDescriptor() {
            return new ContentDescriptor(ContentDescriptor.RAW);
        }

        @Override
        public boolean endOfStream() {
            return eos;
        }

        @Override
        public long getContentLength() {
            return LENGTH_UNKNOWN;
        }

        @Override
        public Format getFormat() {
            return tc.getFormat();
        }

        @Override
        public void setTransferHandler(BufferTransferHandler bth) {
            this.bth = bth;
        }

        @Override
        public Object getControl(String name) {
            // No controls
            return null;
        }

        @Override
        public Object[] getControls() {
            // No controls
            return new Control[0];
        }

        @Override
        public synchronized void transferData(PushBufferStream pbs) {
            processData();
        }

        //----

        /**
         * Utility function to check for raw (linear) audio.
         */
        static boolean isRawAudio(Format format) {
            return (format instanceof AudioFormat) && format.getEncoding().equalsIgnoreCase(AudioFormat.LINEAR);
        }
    }
}
