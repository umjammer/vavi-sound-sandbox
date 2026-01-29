/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.twinvq;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.SourceDataLine;

import vavi.io.OutputEngine;
import vavi.io.OutputEngineInputStream;
import vavi.sound.ilbc.Decoder;
import vavi.sound.twinvq.LibAV.AVCodecContext;
import vavi.sound.twinvq.LibAV.AVFormatContext;
import vavi.sound.twinvq.LibAV.AVFrame;
import vavi.sound.twinvq.LibAV.AVInputFormat;
import vavi.sound.twinvq.LibAV.AVPacket;
import vavi.sound.twinvq.TwinVQ;
import vavi.sound.twinvq.TwinVQDec;
import vavi.sound.twinvq.TwinVQDec.TwinVQContext;

import static java.lang.System.getLogger;
import static vavi.sound.SoundUtil.volume;


/**
 * Converts a TwinVQ BitStream into a PCM 16bits/sample audio stream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260129 nsano initial version <br>
 */
class Twinvq2PcmAudioInputStream extends AudioInputStream {

    private static final Logger logger = getLogger(Twinvq2PcmAudioInputStream.class.getName());

    /** gross */
    private static ThreadLocal<TwinvqOutputEngine> engineStore = new InheritableThreadLocal<>();

    /** gross */
    private static TwinvqOutputEngine init(InputStream in, AVInputFormat twinvq) throws IOException {
        TwinvqOutputEngine engine = new TwinvqOutputEngine(in, twinvq);
        engineStore.set(engine);
        return engine;
    }

    /**
     * Constructor.
     *
     * @param in     the underlying input stream.
     * @param format the target format of this stream's audio data.
     * @param length the length in sample frames of the data in this stream.
     */
    public Twinvq2PcmAudioInputStream(InputStream in, AudioFormat format, long length, AVInputFormat twinvq) throws IOException {
        super(new OutputEngineInputStream(init(in, twinvq)), format, length);
    }

    @Override
    public AudioFormat getFormat() {
        AudioFormat originalFormat = super.getFormat();
        return new AudioFormat(
                originalFormat.getEncoding(),
                engineStore.get().sampleRate,
                16,
                engineStore.get().channels,
                engineStore.get().channels * 2,
                engineStore.get().sampleRate,
                false);
    }

    /** */
    private static class TwinvqOutputEngine implements OutputEngine {

        /** */
        private DataOutputStream out;

        private final AVFrame frame;
        private final AVFormatContext formatContext;
        private final AVInputFormat inputFormat;
        private final AVCodecContext codecContext;

        private final int sampleRate;
        private final int channels;

        /** */
        public TwinvqOutputEngine(InputStream is, AVInputFormat inputFormat) throws IOException {
            this.inputFormat = inputFormat;

            // header
            this.formatContext = new AVFormatContext();
            formatContext.pb = new DataInputStream(is);
            inputFormat.read_header.apply(formatContext);

            // decoder
            this.codecContext = formatContext.streams[0].codecpar;
            codecContext.priv_data = new TwinVQContext();
            TwinVQDec.twinvq_decode_init(codecContext);

            // Audio output setup
            this.sampleRate = codecContext.sample_rate;
            this.channels = codecContext.ch_layout.nb_channels;

            this.frame = new AVFrame();
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
            } else {
                AVPacket packet = inputFormat.read_packet.apply(formatContext);
                if (packet != null) { // End of file
                    int[] got_frame_ptr = new int[1];
                    int r = TwinVQ.ff_twinvq_decode_frame(codecContext, frame, got_frame_ptr, packet);
                    if (r >= 0) { // Decode error

                        if (got_frame_ptr[0] != 0 && frame.extended_data != null) {
                            float[][] audioData = (float[][]) frame.extended_data;
                            int samples = frame.nb_samples;

                            // Convert float to 16-bit PCM and play
                            byte[] pcmData = new byte[samples * channels * 2];
                            for (int i = 0; i < samples; i++) {
                                for (int ch = 0; ch < channels; ch++) {
                                    float sample = audioData[ch][i];
                                    // Clamp to [-1.0, 1.0] and convert to 16-bit
                                    sample = Math.max(-1.0f, Math.min(1.0f, sample));
                                    short s = (short) (sample * 32767);
                                    int idx = (i * channels + ch) * 2;
                                    pcmData[idx] = (byte) (s & 0xff);
                                    pcmData[idx + 1] = (byte) ((s >> 8) & 0xff);
                                }
                            }
                            out.write(pcmData, 0, pcmData.length);
                        }
                    }
                } else {
                    out.close();
                }
            }
        }

        @Override
        public void finish() throws IOException {
            TwinVQ.ff_twinvq_decode_close(codecContext);
        }
    }
}
