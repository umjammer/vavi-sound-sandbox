/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.xma;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import vavi.io.OutputEngine;
import vavi.io.OutputEngineInputStream;
import vavi.sound.xma.XmaContainer;
import vavi.sound.xma.XmaFrame;
import vavi.sound.xma.XmaStreamInfo;
import vavi.sound.xma.WmaProDecoder;


/**
 * Converts a WMA Pro / XMA bitstream into a PCM 16bits/sample audio stream.
 * <p>
 * Mirrors the frame-set decoding loop of Echo's {@code Program.cs}: one
 * {@link WmaProDecoder} per substream, per-stream output buffers concatenated
 * into an interleaved PCM16 (little-endian) stream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260707 nsano initial version <br>
 */
class Xma2PcmAudioInputStream extends AudioInputStream {

    /**
     * Constructor.
     *
     * @param in     the underlying input stream.
     * @param format the target format of this stream's audio data.
     * @param length the length in sample frames of the data in this stream.
     */
    public Xma2PcmAudioInputStream(InputStream in, AudioFormat format, long length) throws IOException {
        super(new OutputEngineInputStream(new WmaOutputEngine(in)), format, length);
    }

    /** */
    private static class WmaOutputEngine implements OutputEngine {

        /** */
        private DataOutputStream out;

        private final XmaStreamInfo info;
        private final vavi.sound.xma.XmaFrameReader reader;
        private final WmaProDecoder[] decoders;
        private final float[][][] streamOutputs;
        private final float[][] combined;
        private final XmaFrame[] frameSet;
        private final int channels;
        private final int samplesPerFrame;

        /** */
        public WmaOutputEngine(InputStream is) throws IOException {
            byte[] data = is.readAllBytes();
            XmaContainer container = XmaContainer.open(data);
            this.info = container.streamInfo();
            this.reader = new vavi.sound.xma.XmaFrameReader(container);

            this.decoders = new WmaProDecoder[info.numStreams];
            this.streamOutputs = new float[info.numStreams][][];
            for (int s = 0; s < info.numStreams; s++) {
                decoders[s] = new WmaProDecoder(info, s);
                streamOutputs[s] = new float[decoders[s].channels()][];
                for (int c = 0; c < decoders[s].channels(); c++) {
                    streamOutputs[s][c] = new float[decoders[s].samplesPerFrame()];
                }
            }

            this.channels = info.channels;
            this.samplesPerFrame = decoders[0].samplesPerFrame();

            this.combined = new float[channels][];
            for (int c = 0; c < channels; c++) {
                combined[c] = new float[samplesPerFrame];
            }

            this.frameSet = new XmaFrame[info.numStreams];
        }

        @Override
        public void initialize(OutputStream out) throws IOException {
            if (this.out != null) {
                throw new IOException("Already initialized");
            } else {
                this.out = new DataOutputStream(out);
            }
        }

        @Override
        public void execute() throws IOException {
            if (out == null) {
                throw new IOException("Not yet initialized");
            } else if (reader.readFrameSet(frameSet)) {
                int samples = samplesPerFrame;
                int channelOffset = 0;
                for (int s = 0; s < info.numStreams; s++) {
                    decoders[s].decodeFrame(frameSet[s], streamOutputs[s]);
                    for (int c = 0; c < decoders[s].channels(); c++) {
                        System.arraycopy(streamOutputs[s][c], 0, combined[channelOffset + c], 0, samples);
                    }
                    channelOffset += decoders[s].channels();
                }

                byte[] pcm = new byte[samples * channels * 2];
                int pos = 0;
                for (int n = 0; n < samples; n++) {
                    for (int c = 0; c < channels; c++) {
                        float v = combined[c][n];
                        if (v > 1.0f) {
                            v = 1.0f;
                        } else if (v < -1.0f) {
                            v = -1.0f;
                        }
                        short s = (short) (v * 32767.0f);
                        pcm[pos++] = (byte) (s & 0xFF);
                        pcm[pos++] = (byte) ((s >> 8) & 0xFF);
                    }
                }
                out.write(pcm, 0, pcm.length);
            } else {
                out.close();
            }
        }

        @Override
        public void finish() throws IOException {
        }
    }
}
