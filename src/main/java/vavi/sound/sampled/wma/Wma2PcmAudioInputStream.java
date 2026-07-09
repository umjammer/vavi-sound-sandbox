/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.wma;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import vavi.io.OutputEngine;
import vavi.io.OutputEngineInputStream;
import vavi.sound.wma.AsfDemuxer;
import vavi.sound.wma.AsfInfo;
import vavi.sound.wma.WmaDecoder;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.getLogger;


/**
 * Converts a WMA v1/v2 (ASF) bitstream into a PCM 16-bit/sample audio stream.
 * <p>
 * Mirrors the decode loop of the {@code @audio/wma-decode} npm package: ASF
 * demux, split each data-packet payload into {@code blockAlign}-sized WMA
 * superframes, decode each with a single {@link WmaDecoder}, then interleave the
 * planar float output to little-endian PCM16.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260707 nsano initial version <br>
 */
class Wma2PcmAudioInputStream extends AudioInputStream {

    /**
     * Constructor.
     *
     * @param in     the underlying WMA/ASF input stream.
     * @param format the target PCM format of this stream's audio data.
     * @param length the length in sample frames of the data in this stream.
     */
    public Wma2PcmAudioInputStream(InputStream in, AudioFormat format, long length) throws IOException {
        super(new OutputEngineInputStream(new WmaOutputEngine(in)), format, length);
    }

    /** */
    private static class WmaOutputEngine implements OutputEngine {

        private static final Logger logger = getLogger(WmaOutputEngine.class.getName());

        /** */
        private DataOutputStream out;

        private final WmaDecoder decoder;
        private final int channels;
        private final List<byte[]> frames;
        private int frameIndex;
        private boolean flushed;

        /** */
        public WmaOutputEngine(InputStream is) throws IOException {
            byte[] data = is.readAllBytes();
            AsfInfo info = AsfDemuxer.demux(data);
            this.decoder = new WmaDecoder(info);
            this.channels = decoder.channels();

            // de-packetise into blockAlign-sized WMA superframes
            this.frames = new ArrayList<>();
            int ba = info.blockAlign;
            for (byte[] pkt : info.packets) {
                for (byte[] payload : AsfDemuxer.parsePacket(pkt)) {
                    for (int off = 0; off + ba <= payload.length; off += ba) {
                        frames.add(Arrays.copyOfRange(payload, off, off + ba));
                    }
                }
            }
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
            }
            if (frameIndex < frames.size()) {
                float[][] planar;
                try {
                    planar = decoder.decodeSuperframe(frames.get(frameIndex));
                } catch (RuntimeException e) {
logger.log(DEBUG, "frame " + frameIndex + ": " + e);
                    planar = null;
                }
                frameIndex++;
                if (planar != null) {
                    writeInterleaved(planar);
                }
            } else if (!flushed) {
                flushed = true;
                writeInterleaved(decoder.flush());
            } else {
                out.close();
            }
        }

        /** Clamps planar float samples to [-1, 1] and writes interleaved LE PCM16. */
        private void writeInterleaved(float[][] planar) throws IOException {
            int samples = planar.length > 0 ? planar[0].length : 0;
            if (samples == 0) {
                return;
            }
            byte[] pcm = new byte[samples * channels * 2];
            int pos = 0;
            for (int n = 0; n < samples; n++) {
                for (int c = 0; c < channels; c++) {
                    float v = planar[c][n];
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
        }

        @Override
        public void finish() throws IOException {
        }
    }
}
