/*
 * Copyright (c) 2001,2006,2008 by Florian Bomers <http://www.bomers.de>
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

package vavi.sound.pcm.resampling.tritonus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;

import vavi.util.Debug;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_UNSIGNED;
import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;


/**
 * This provider converts sample rate of 2 PCM streams. <br>
 * It does:
 * <ul>
 * <li>conversion of different sample rates
 * <li>conversion of unsigned/signed (only 8bit unsigned supported)
 * <li>conversion of big/small endian
 * <li>8,16,24,32 bit conversion
 * </ul>
 * It does NOT:
 * <ul>
 * <li>change channel count
 * <li>accept a stream where the sample rates are equal. This case should be
 * handled by the PCM2PCM converter
 * </ul>
 * 
 * @author Florian Bomers
 */
public class SampleRateConversionProvider extends FormatConversionProvider {

    /** */
    private static final AudioFormat[] OUTPUT_FORMATS = {
        // Encoding, SampleRate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian
        new AudioFormat(PCM_SIGNED, NOT_SPECIFIED, 8, NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED, false),
        new AudioFormat(PCM_SIGNED, NOT_SPECIFIED, 8, NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED, true),
        new AudioFormat(PCM_UNSIGNED, NOT_SPECIFIED, 8, NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED, false),
        new AudioFormat(PCM_UNSIGNED, NOT_SPECIFIED, 8, NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED, true),
        new AudioFormat(PCM_SIGNED, NOT_SPECIFIED, 16, NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED, false),
        new AudioFormat(PCM_SIGNED, NOT_SPECIFIED, 16, NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED, true),
        new AudioFormat(PCM_SIGNED, NOT_SPECIFIED, 24, NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED, false),
        new AudioFormat(PCM_SIGNED, NOT_SPECIFIED, 24, NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED, true),
        new AudioFormat(PCM_SIGNED, NOT_SPECIFIED, 32, NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED, false),
        new AudioFormat(PCM_SIGNED, NOT_SPECIFIED, 32, NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED, true),
    };

    /** */
    private static boolean containsFormat(AudioFormat sourceFormat, List<AudioFormat> possibleFormats) {
        for (AudioFormat format : possibleFormats) {
            if (AudioFormats.matches(format, sourceFormat)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Constructor.
     */
    public SampleRateConversionProvider() {
Debug.println("here 0");
        sourceEncodings = new ArrayList<>();
        targetEncodings = new ArrayList<>();
        sourceFormats = Arrays.asList(OUTPUT_FORMATS);
        targetFormats = Arrays.asList(OUTPUT_FORMATS);
        collectEncodings(sourceFormats, sourceEncodings);
        collectEncodings(targetFormats, targetEncodings);
    }

    /** */
    private static void collectEncodings(List<AudioFormat> formats, List<AudioFormat.Encoding> encodings) {
        for (AudioFormat format : formats) {
            encodings.add(format.getEncoding());
        }
    }

    /** $$fb2000-10-04: use AudioSystem.NOT_SPECIFIED for all fields. */
    public AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding, AudioInputStream audioInputStream) {
        AudioFormat sourceFormat = audioInputStream.getFormat();
        AudioFormat targetFormat = new AudioFormat(targetEncoding,
                                                   NOT_SPECIFIED,   // sample rate
                                                   NOT_SPECIFIED,   // sample size in bits
                                                   NOT_SPECIFIED,   // channels
                                                   NOT_SPECIFIED,   // frame size
                                                   NOT_SPECIFIED,   // frame rate
                                                   sourceFormat.isBigEndian()); // big endian
Debug.println("TFormatConversionProvider.getAudioInputStream(AudioFormat.Encoding, AudioInputStream):");
Debug.println("trying to convert to " + targetFormat);
        return getAudioInputStream(targetFormat, audioInputStream);
    }

    /** */
    public AudioFormat.Encoding[] getSourceEncodings() {
        return sourceEncodings.toArray(new AudioFormat.Encoding[sourceEncodings.size()]);
    }

    /** */
    public AudioFormat.Encoding[] getTargetEncodings() {
        return targetEncodings.toArray(new AudioFormat.Encoding[targetEncodings.size()]);
    }

    /**
     *  This implementation assumes that the converter can convert
     *  from each of its source encodings to each of its target
     *  encodings. If this is not the case, the converter has to
     *  override this method.
     */
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
Debug.println("here 1");
        if (isAllowedSourceFormat(sourceFormat)) {
            return getTargetEncodings();
        } else {
            return new AudioFormat.Encoding[0];
        }
    }

    /** */
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream) {
        AudioFormat sourceFormat = sourceStream.getFormat();

        // the non-conversion case
        if (AudioFormats.matches(sourceFormat, targetFormat)) {
            return sourceStream;
        }

        targetFormat = replaceNotSpecified(sourceFormat, targetFormat);
        // do not support NOT_SPECIFIED as sample rates
        if (targetFormat.getSampleRate() != NOT_SPECIFIED &&
            sourceFormat.getSampleRate() != NOT_SPECIFIED &&
            targetFormat.getChannels() != NOT_SPECIFIED &&
            sourceFormat.getChannels() != NOT_SPECIFIED &&
            targetFormat.getSampleSizeInBits() != NOT_SPECIFIED &&
            sourceFormat.getSampleSizeInBits() != NOT_SPECIFIED &&
            isConversionSupported(sourceFormat, targetFormat)) {
            return new SampleRateConverterStream(sourceStream, targetFormat);
        }
        throw new IllegalArgumentException("format conversion not supported");
    }

    /**
     * replaces the sample rate and frame rate.
     * Should only be used with PCM_SIGNED or PCM_UNSIGNED
     */
    private static AudioFormat replaceSampleRate(AudioFormat format, float newSampleRate) {
        if (format.getSampleRate() == newSampleRate) {
            return format;
        }
        return new AudioFormat(format.getEncoding(), newSampleRate, format.getSampleSizeInBits(), format.getChannels(), format.getFrameSize(), newSampleRate, format.isBigEndian());
    }

    /** */
    private static final float[] commonSampleRates = {
        8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000, 64000, 88200, 96000, 192000
    };

    /** */
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
Debug.println(">SampleRateConversionProvider.getTargetFormats(AudioFormat.Encoding, AudioFormat):");
Debug.println("checking out possible target formats");
Debug.println("from: " + sourceFormat);
Debug.println("to  : " + targetEncoding);
        float sourceSampleRate = sourceFormat.getSampleRate();
        // a trick: set sourceFormat's sample rate to -1 so that
        // replaceNotSpecified does not replace the sample rate.
        // we want to convert that !
        sourceFormat = replaceSampleRate(sourceFormat, NOT_SPECIFIED);
        if (isConversionSupported(targetEncoding, sourceFormat)) {
            List<AudioFormat> result = new ArrayList<>();
            for (AudioFormat targetFormat : targetFormats) {
                targetFormat = replaceNotSpecified(sourceFormat, targetFormat);
                if (isConversionSupported(targetFormat, sourceFormat)) {
                    result.add(targetFormat);
                }
            }
            // for convenience, add some often used sample rates as output
            // this may help applications that do not handle NOT_SPECIFIED
            if (result.size() > 0 && sourceSampleRate != NOT_SPECIFIED) {
                for (AudioFormat format : result) {
                    for (float commonSampleRate : commonSampleRates) {
                        if (!doMatch(sourceSampleRate, commonSampleRate)) {
                            result.add(replaceSampleRate(format, commonSampleRate));
                        }
                    }
                }
            }
Debug.println("<found " + result.size() + " matching formats.");
            return result.toArray(new AudioFormat[result.size()]);
        } else {
Debug.println("<returning empty array.");
            return new AudioFormat[0];
        }
    }

    /** */
    @Override
    public boolean isConversionSupported(AudioFormat targetFormat, AudioFormat sourceFormat) {
        // do not match when targetSampleRate set and sourceSamplerate set and
        // NOT both the same
        boolean result = (targetFormat.getSampleRate() == NOT_SPECIFIED ||
                targetFormat.getSampleRate() == NOT_SPECIFIED ||
                !doMatch(targetFormat.getSampleRate(), sourceFormat.getSampleRate()) &&
                doMatch(targetFormat.getChannels(), sourceFormat.getChannels())) &&
                containsFormat(sourceFormat, sourceFormats) &&
                containsFormat(targetFormat, targetFormats);
Debug.println(">SampleRateConverter: isConversionSupported(AudioFormat, AudioFormat):");
Debug.println("checking if conversion possible");
Debug.println("from: " + sourceFormat);
Debug.println("to  : " + targetFormat);
Debug.println("< result : " + result);
        return result;
    }

    /** */
    private static long convertLength(AudioFormat sourceFormat, AudioFormat targetFormat, long sourceLength) {
        if (sourceLength == NOT_SPECIFIED) {
            return sourceLength;
        }
        return Math.round(targetFormat.getSampleRate() / sourceFormat.getSampleRate() * sourceLength);
    }

    /**
     * SampleRateConverterStream.
     * <p>
     * at the moment, there are so many special things to care about, and new
     * things in an AIS, that I derive directly from AudioInputStream. I cannot
     * use TAsynchronousFilteredAudioInputStream because
     * </p>
     * <ul>
     * <li> it doesn't allow convenient use of a history. The history will be
     * needed especially when performing filtering
     * <li> it doesn't work on FloatSampleBuffer (yet)
     * <li> each sample must be calculated one-by-one. The asynchronous
     * difficulty isn't overcome by using a TCircularBuffer I cannot use
     * TSynchronousFilteredAudioInputStream because
     * <li> it doesn't handle different sample rates
     * </ul>
     * <p>
     * Later we can make a base class for this, e.g. THistoryAudioInputStream
     * </p>
     * TODO: when target sample rate is < source sample rate (or only slightly
     * above), this stream calculates ONE sample too much.
     */
    public static class SampleRateConverterStream extends AudioInputStream {

        /** the current working buffer with samples of the sourceStream */
        private FloatSampleBuffer thisBuffer = null;

        /** used when read(byte[],int,int) is called */
        private FloatSampleBuffer writeBuffer = null;

        private byte[] byteBuffer; // used for reading samples of sourceStream

        private AudioInputStream sourceStream;

        private float sourceSampleRate;

        private float targetSampleRate;

        /** index in thisBuffer */
        private double dPos;

        /** source stream is read in buffers of this size - in milliseconds */
        private int sourceBufferTime;

        /** the current conversion algorithm */
        private SampleRateConverter conversionAlgorithm = SampleRateConverter.Factory.getInstance();

        // History support
        /** the buffer with history samples */
        private FloatSampleBuffer historyBuffer = null;

        /**
         * the minimum number of samples that must be present in the history
         * buffer
         */
        private int minimumSamplesInHistory = 1;

        /** force to discard current contents in thisBuffer if true */
        private boolean thisBufferValid = false;

        public SampleRateConverterStream(AudioInputStream sourceStream, AudioFormat targetFormat) {
            // clean up targetFormat:
            // - ignore frame rate totally
            // - recalculate frame size
            super(sourceStream, new SRCAudioFormat(targetFormat), convertLength(sourceStream.getFormat(), targetFormat, sourceStream.getFrameLength()));
Debug.println("SampleRateConverterStream: <init>");
            this.sourceStream = sourceStream;
            sourceSampleRate = sourceStream.getFormat().getSampleRate();
            targetSampleRate = targetFormat.getSampleRate();
            dPos = 0;
            // use a buffer size of 100ms
            sourceBufferTime = 100;
            resizeBuffers();
            flush(); // force read of source stream next time read is called
        }

        /** */
        private static long millis2Frames(long ms, float frameRate) {
            return (long) (ms * frameRate / 1000);
        }

        /**
         * Assures that both historyBuffer and working buffer
         * <ul>
         * <li>exist
         * <li>have about <code>sourceBufferTime</code> ms samples
         * <li>that both have at least <code>minimumSamplesInHistory</code>
         * samples
         * </ul>
         * This method must be called when anything is changed that may change
         * the size of the buffers.
         */
        private synchronized void resizeBuffers() {
            int bufferSize = (int) millis2Frames(sourceBufferTime, sourceSampleRate);
            if (bufferSize < minimumSamplesInHistory) {
                bufferSize = minimumSamplesInHistory;
            }
            // we must be able to calculate at least one output sample from
            // one input buffer block
            if (bufferSize < outSamples2inSamples(1)) {
                bufferSize = (int) Math.ceil(outSamples2inSamples(1));
            }
            if (historyBuffer == null) {
                historyBuffer = new FloatSampleBuffer(sourceStream.getFormat().getChannels(), bufferSize, sourceSampleRate);
                historyBuffer.makeSilence();
            }
            // TODO: retain last samples !
            historyBuffer.changeSampleCount(bufferSize, true);
            if (thisBuffer == null) {
                thisBuffer = new FloatSampleBuffer(sourceStream.getFormat().getChannels(), bufferSize, sourceSampleRate);
            }
            // TODO: retain last samples and adjust dPos
            thisBuffer.changeSampleCount(bufferSize, true);
        }

        /**
         * Reads from a source stream that cannot handle float buffers. After
         * this method has been called, it is to be checked whether we are
         * closed !
         */
        private synchronized void readFromByteSourceStream() throws IOException {
            int byteCount = thisBuffer.getByteArrayBufferSize(getFormat());
            if (byteBuffer == null || byteBuffer.length < byteCount) {
                byteBuffer = new byte[byteCount];
            }
            // finally read it
            int bytesRead = 0;
            int thisRead;
            do {
                thisRead = sourceStream.read(byteBuffer, bytesRead, byteCount - bytesRead);
                if (thisRead > 0) {
                    bytesRead += thisRead;
                }
            } while (bytesRead < byteCount && thisRead > 0);
            if (bytesRead == 0) {
                // sourceStream is closed. We don't accept 0 bytes read from
                // source stream
                close();
            } else {
                thisBuffer.initFromByteArray(byteBuffer, 0, bytesRead, sourceStream.getFormat());
            }
        }

        /** */
//        private synchronized void readFromFloatSourceStream() throws IOException {
            // ((FloatSampleInput) sourceStream).read(thisBuffer);
//        }

        /** */
        private long testInFramesRead = 0;

        /** */
//private long testOutFramesReturned = 0;

        /**
         * fills thisBuffer with new samples. It sets the history buffer to the
         * last buffer. thisBuffer's sampleCount will be the number of samples
         * read. Calling methods MUST check whether this stream is closed upon
         * completion of this method. If the stream is closed, the contents of
         * <code>thisBuffer</code> are not valid.
         */
        private synchronized void readFromSourceStream() throws IOException {
            if (isClosed()) {
                return;
            }
            int oldSampleCount = thisBuffer.getSampleCount();
            // reuse history buffer
            FloatSampleBuffer newBuffer = historyBuffer;
            historyBuffer = thisBuffer;
            thisBuffer = newBuffer;
            if (sourceStream.getFrameLength() != NOT_SPECIFIED && thisBuffer.getSampleCount() + testInFramesRead > sourceStream.getFrameLength()) {
                if (sourceStream.getFrameLength() - testInFramesRead <= 0) {
                    close();
                    return;
                }
                thisBuffer.changeSampleCount((int) (sourceStream.getFrameLength() - testInFramesRead), false);
            }

            // if (sourceStream instanceof FloatSampleInput) {
            // readFromFloatSourceStream();
            // } else {
            readFromByteSourceStream();
            // }
//testInFramesRead += thisBuffer.getSampleCount();
//Debug.println("Read " + thisBuffer.getSampleCount() + " frames from source stream. Total=" + testInFramesRead);
            double inc = outSamples2inSamples(1.0);
            if (!thisBufferValid) {
                thisBufferValid = true;
                // dPos=inc/2;
                dPos = 0.0;
            } else {
//double temp = dPos;
                dPos -= oldSampleCount;
//Debug.println("new dPos: " + temp + " - " + oldSampleCount + " = " + dPos);
                if ((dPos > inc || dPos < -inc) && ((int) Math.floor(dPos)) != 0) {
                    // hard-reset dPos if - why ever - it got out of bounds
Debug.println("Need to hard reset dPos=" + dPos + " !!!!!!!!!!!!!!!!!!!!!!!");
                    dPos = 0.0;
                }
            }
        }

        private double inSamples2outSamples(double inSamples) {
            return inSamples * targetSampleRate / sourceSampleRate;
        }

        private double outSamples2inSamples(double outSamples) {
            return outSamples * sourceSampleRate / targetSampleRate;
        }

        /**
         * Main read method. It blocks until all samples are converted or the
         * source stream is at its end or closed.<br>
         * The sourceStream's sample rate is converted following the current
         * setting of <code>conversionAlgorithm</code>. At most
         * outBuffer.getSampleCount() are converted. In general, if the return
         * value (and outBuffer.getSampleCount()) is less after processing this
         * function, then it is an indicator that it was the last block to be
         * processed.
         * 
         * @see #setConversionAlgorithm(SampleRateConverter)
         * @param outBuffer the buffer that the converted samples will be
         *            written to.
         * @throws IllegalArgumentException when outBuffer's channel count does
         *             not match
         * @return number of samples in outBuffer ( ==
         *         outBuffer.getSampleCount()) or -1. A return value of 0 is
         *         only possible when outBuffer has 0 samples.
         */
        public synchronized int read(FloatSampleBuffer outBuffer) throws IOException {
            if (isClosed()) {
                return -1;
            }
            if (outBuffer.getChannelCount() != thisBuffer.getChannelCount()) {
                throw new IllegalArgumentException("passed buffer has different channel count");
            }
            if (outBuffer.getSampleCount() == 0) {
                return 0;
            }
//Debug.println(">SamplerateConverterStream.read(" + outBuffer.getSampleCount() + "frames)");
            float[] outSamples;
            float[] inSamples;
            float[] history;
            double increment = outSamples2inSamples(1.0);
            int writtenSamples = 0;
            do {
                // check thisBuffer with samples of source stream
                int inSampleCount = thisBuffer.getSampleCount();
                if (((int) Math.floor(dPos)) >= inSampleCount || !thisBufferValid) {
                    // need to load new data of sourceStream
                    readFromSourceStream();
                    if (isClosed()) {
                        break;
                    }
                    inSampleCount = thisBuffer.getSampleCount();
                }
                // calculate number of samples to write
                int writeCount = outBuffer.getSampleCount() - writtenSamples;
                // check whether this exceeds the current in-buffer
                if (((int) Math.floor(outSamples2inSamples(writeCount) + dPos)) >= inSampleCount) {
                    int lastOutIndex = (int) Math.ceil(inSamples2outSamples(inSampleCount - dPos));
                    // normally, the above formula gives the exact writeCount.
                    // but due to rounding issues, sometimes it has to be
                    // decremented once.
                    // so we need to iterate to get the last index and then
                    // increment it once to make
                    // it the writeCount (=the number of samples to write)
                    while (((int) Math.floor(outSamples2inSamples(lastOutIndex) + dPos)) >= inSampleCount) {
                        lastOutIndex--;
//Debug.println("--------- Decremented lastOutIndex=" + lastOutIndex);
                    }
int testLastOutIndex = writeCount - 1;
while (((int) Math.floor(outSamples2inSamples(testLastOutIndex) + dPos)) >= inSampleCount) {
 testLastOutIndex--;
}
if (testLastOutIndex != lastOutIndex) {
 Debug.println("lastOutIndex wrong: lastOutIndex=" + lastOutIndex + " testLastOutIndex=" + testLastOutIndex + " !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
}
                    writeCount = lastOutIndex + 1;
                }
                // finally do the actual conversion - separated per channel
                for (int channel = 0; channel < outBuffer.getChannelCount(); channel++) {
                    inSamples = thisBuffer.getChannel(channel);
                    outSamples = outBuffer.getChannel(channel);
                    history = historyBuffer.getChannel(channel);
                    conversionAlgorithm.convert(inSamples, dPos, inSampleCount, increment, outSamples, writtenSamples, writeCount, history, historyBuffer.getSampleCount());
                }
                writtenSamples += writeCount;
                // adjust new position
                dPos += outSamples2inSamples(writeCount);
            } while (!isClosed() && writtenSamples < outBuffer.getSampleCount());
            if (writtenSamples < outBuffer.getSampleCount()) {
                outBuffer.changeSampleCount(writtenSamples, true);
            }
//testOutFramesReturned += outBuffer.getSampleCount();
//Debug.println("< return " + outBuffer.getSampleCount() + "frames. Total=" + testOutFramesReturned + " frames. Read total " + testInFramesRead + " frames from source stream");
            return outBuffer.getSampleCount();
        }

        // utility methods

        /** */
        protected double sourceFrames2targetFrames(double sourceFrames) {
            return targetSampleRate / sourceSampleRate * sourceFrames;
        }

        /** */
        protected double targetFrames2sourceFrames(double targetFrames) {
            return sourceSampleRate / targetSampleRate * targetFrames;
        }

        /** */
        protected long sourceBytes2targetBytes(long sourceBytes) {
            long sourceFrames = sourceBytes / getSourceFrameSize();
            long targetFrames = (long) sourceFrames2targetFrames(sourceFrames);
            return targetFrames * getFrameSize();
        }

        /** */
        protected long targetBytes2sourceBytes(long targetBytes) {
            long targetFrames = targetBytes / getFrameSize();
            long sourceFrames = (long) targetFrames2sourceFrames(targetFrames);
            return sourceFrames * getSourceFrameSize();
        }

        /** */
        public int getFrameSize() {
            return getFormat().getFrameSize();
        }

        /** */
        public int getSourceFrameSize() {
            return sourceStream.getFormat().getFrameSize();
        }

        // methods overwritten of AudioInputStream

        /** */
        public int read() throws IOException {
            if (getFormat().getFrameSize() != 1) {
                throw new IOException("frame size must be 1 to read a single byte");
            }
            // very ugly, but efficient. Who uses this method anyway ?
            byte[] temp = new byte[1];
            int result = read(temp);
            if (result <= 0) {
                return -1;
            }
            return temp[0] & 0xff;
        }

        /**
         * @see #read(byte[], int, int)
         */
        public int read(byte[] abData) throws IOException {
            return read(abData, 0, abData.length);
        }

        /**
         * Read nLength bytes that will be the converted samples of the original
         * inputStream. When nLength is not an integral number of frames, this
         * method may read less than nLength bytes.
         */
        public int read(byte[] abData, int nOffset, int nLength) throws IOException {
            if (isClosed()) {
                return -1;
            }
            int frameCount = nLength / getFrameSize();
            if (writeBuffer == null) {
                writeBuffer = new FloatSampleBuffer(getFormat().getChannels(), frameCount, getFormat().getSampleRate());
            } else {
                writeBuffer.changeSampleCount(frameCount, false);
            }
            int writtenSamples = read(writeBuffer);
            if (writtenSamples == -1) {
                return -1;
            }
            int written = writeBuffer.convertToByteArray(abData, nOffset, getFormat());
            return written;
        }

        /** */
        public synchronized long skip(long nSkip) throws IOException {
            // only returns integral frames
            long sourceSkip = targetBytes2sourceBytes(nSkip);
            long sourceSkipped = sourceStream.skip(sourceSkip);
            flush();
            return sourceBytes2targetBytes(sourceSkipped);
        }

        /** */
        public int available() throws IOException {
            return (int) sourceBytes2targetBytes(sourceStream.available());
        }

        /** */
        public void mark(int readlimit) {
            sourceStream.mark((int) targetBytes2sourceBytes(readlimit));
        }

        /** */
        public synchronized void reset() throws IOException {
            sourceStream.reset();
            flush();
        }

        /** */
        public boolean markSupported() {
            return sourceStream.markSupported();
        }

        /** */
        public void close() throws IOException {
            if (isClosed()) {
                return;
            }
            sourceStream.close();
            // clean memory, this will also be an indicator that
            // the stream is closed
            thisBuffer = null;
            historyBuffer = null;
            byteBuffer = null;
        }

        // additional methods

        /** */
        public boolean isClosed() {
            return thisBuffer == null;
        }

        /**
         * Flushes the internal buffers
         */
        public synchronized void flush() {
            if (!isClosed()) {
                thisBufferValid = false;
                historyBuffer.makeSilence();
            }
        }

        // Properties

        /** */
        public synchronized void setTargetSampleRate(float sr) {
            if (sr > 0) {
                targetSampleRate = sr;
                // ((SRCAudioFormat) getFormat()).setSampleRate(sr);
                resizeBuffers();
            }
        }

        /** */
        public synchronized void setConversionAlgorithm(SampleRateConverter algo) {
            conversionAlgorithm = algo;
            resizeBuffers();
        }

        /** */
        public synchronized float getTargetSampleRate() {
            return targetSampleRate;
        }

        /** */
        public synchronized SampleRateConverter getConversionAlgorithm() {
            return conversionAlgorithm;
        }
    }

    /**
     * Obviously, this class is used to be able to set the frame rate/sample
     * rate after the AudioFormat object has been created. It assumes the PCM
     * case where the frame rate is always in sync with the sample rate. (MP)
     */
    public static class SRCAudioFormat extends AudioFormat {
        /** */
        private float sampleRate;

        /** */
        public SRCAudioFormat(AudioFormat targetFormat) {
            super(targetFormat.getEncoding(), targetFormat.getSampleRate(), targetFormat.getSampleSizeInBits(), targetFormat.getChannels(), targetFormat.getChannels() * targetFormat.getSampleSizeInBits() / 8, targetFormat.getSampleRate(), targetFormat.isBigEndian());
            this.sampleRate = targetFormat.getSampleRate();
        }

        /** */
        public void setSampleRate(float sr) {
            if (sr > 0) {
                this.sampleRate = sr;
            }
        }

        /** */
        public float getSampleRate() {
            return this.sampleRate;
        }

        /** */
        public float getFrameRate() {
            return this.sampleRate;
        }
    }

    //----

    /** */
    private List<AudioFormat.Encoding> sourceEncodings;
    /** */
    private List<AudioFormat.Encoding> targetEncodings;
    /** */
    private List<AudioFormat> sourceFormats;
    /** */
    private List<AudioFormat> targetFormats;

    /** */
    protected boolean isAllowedSourceFormat(AudioFormat sourceFormat) {
        for (AudioFormat format : sourceFormats) {
Debug.println("here: " + format);
            if (AudioFormats.matches(format, sourceFormat)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Utility method to check whether these values match, 
     * taking into account AudioSystem.NOT_SPECIFIED.
     * @return true if any of the values is AudioSystem.NOT_SPECIFIED 
     * or both values have the same value.
     */
    protected static boolean doMatch(int i1, int i2) {
        return i1 == AudioSystem.NOT_SPECIFIED ||
               i2 == AudioSystem.NOT_SPECIFIED ||
               i1 == i2;
    }

    /**
     * @see #doMatch(int,int)
     */
    protected static boolean doMatch(float f1, float f2) {
        return f1 == AudioSystem.NOT_SPECIFIED ||
               f2 == AudioSystem.NOT_SPECIFIED ||
               Math.abs(f1 - f2) < 1.0e-9;
    }

    /**
     * Utility method, replaces all occurrences of AudioSystem.NOT_SPECIFIED
     * in <code>targetFormat</code> with the corresponding value in <code>sourceFormat</code>.
     * If <code>targetFormat</code> does not contain any fields with AudioSystem.NOT_SPECIFIED,
     * it is returned unmodified. The endian-ness and encoding remain the same in all cases.
     * <p>
     * If any of the fields is AudioSystem.NOT_SPECIFIED in both <code>sourceFormat</code> and 
     * <code>targetFormat</code>, it will remain not specified.
     * <p>
     * This method uses <code>getFrameSize(...)</code> (see below) to set the new frameSize, 
     * if a new AudioFormat instance is created.
     * <p>
     * This method isn't used in TSimpleFormatConversionProvider - it is solely there
     * for inheriting classes.
     */
    protected AudioFormat replaceNotSpecified(AudioFormat sourceFormat, AudioFormat targetFormat) {
        boolean toSetSampleSize = false;
        boolean toSetChannels = false;
        boolean toSetSampleRate = false;
        boolean toSetFrameRate = false;
        if (targetFormat.getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED &&
            sourceFormat.getSampleSizeInBits() != AudioSystem.NOT_SPECIFIED) {
            toSetSampleSize = true;
        }
        if (targetFormat.getChannels() == AudioSystem.NOT_SPECIFIED &&
            sourceFormat.getChannels() != AudioSystem.NOT_SPECIFIED) {
            toSetChannels = true;
        }
        if (targetFormat.getSampleRate() == AudioSystem.NOT_SPECIFIED &&
            sourceFormat.getSampleRate() != AudioSystem.NOT_SPECIFIED) {
            toSetSampleRate = true;
        }
        if (targetFormat.getFrameRate() == AudioSystem.NOT_SPECIFIED &&
            sourceFormat.getFrameRate() != AudioSystem.NOT_SPECIFIED) {
            toSetFrameRate = true;
        }
        if (toSetSampleSize || toSetChannels || toSetSampleRate || toSetFrameRate ||
            (targetFormat.getFrameSize() == AudioSystem.NOT_SPECIFIED &&
             sourceFormat.getFrameSize() != AudioSystem.NOT_SPECIFIED)) {
            // create new format in place of the original target format
            float sampleRate = toSetSampleRate ? sourceFormat.getSampleRate() : targetFormat.getSampleRate();
            float frameRate = toSetFrameRate ? sourceFormat.getFrameRate() : targetFormat.getFrameRate();
            int sampleSize = toSetSampleSize ? sourceFormat.getSampleSizeInBits() : targetFormat.getSampleSizeInBits();
            int channels = toSetChannels ? sourceFormat.getChannels() : targetFormat.getChannels();
            int frameSize = getFrameSize(targetFormat.getEncoding(), sampleRate, sampleSize, channels, frameRate, targetFormat.isBigEndian(), targetFormat.getFrameSize());
            targetFormat = new AudioFormat(targetFormat.getEncoding(), sampleRate, sampleSize, channels, frameSize, frameRate, targetFormat.isBigEndian());
        }
        return targetFormat;
    }

    /**
     * Calculates the frame size for the given format description. The default
     * implementation returns AudioSystem.NOT_SPECIFIED if either
     * <code>sampleSize</code> or <code>channels</code> is
     * AudioSystem.NOT_SPECIFIED, otherwise <code>sampleSize*channels/8</code>
     * is returned.
     * <p>
     * If this does not reflect the way to calculate the right frame size,
     * inheriting classes should overwrite this method if they use
     * replaceNotSpecified(...). It is not used elsewhere in this class.
     */
    protected int getFrameSize(AudioFormat.Encoding encoding,
                               float sampleRate,
                               int sampleSize,
                               int channels,
                               float frameRate,
                               boolean bigEndian,
                               int oldFrameSize) {
        if (sampleSize == AudioSystem.NOT_SPECIFIED || channels == AudioSystem.NOT_SPECIFIED) {
            return AudioSystem.NOT_SPECIFIED;
        }
        return sampleSize * channels / 8;
    }
}

/* */
