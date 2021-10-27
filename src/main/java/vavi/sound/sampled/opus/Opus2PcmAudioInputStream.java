/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.opus;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import org.concentus.OpusDecoder;
import org.concentus.OpusException;
import org.gagravarr.opus.OpusAudioData;
import org.gagravarr.opus.OpusFile;

import vavi.io.OutputEngine;
import vavi.io.OutputEngineInputStream;


/**
 * Converts an Opus bitstream into a PCM 16bits/sample audio stream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 111022 nsano initial version <br>
 */
class Opus2PcmAudioInputStream extends AudioInputStream {

    /**
     * Constructor.
     *
     * @param in the underlying input stream.
     * @param format the target format of this stream's audio data.
     * @param length the length in sample frames of the data in this stream.
     */
    public Opus2PcmAudioInputStream(OpusFile in, AudioFormat format, long length) throws IOException {
        super(new OutputEngineInputStream(new OpusOutputEngine(in, format.getSampleRate(), format.getChannels())), format, length);
    }

    /** OGG only */
    private static class OpusOutputEngine implements OutputEngine {

        /** */
        private DataOutputStream out;

        /** */
        private OpusDecoder decoder;

        /** OGG only */
        private OpusFile in;

        /** */
        private int channels;

        /** */
        public OpusOutputEngine(OpusFile in, float sampleRate, int channels) throws IOException {
            try {
                decoder = new OpusDecoder((int) sampleRate, channels);
            } catch (OpusException e) {
                throw new IOException(e);
            }
            this.in = in;
            this.channels = channels;
        }

        /** */
        public void initialize(OutputStream out) throws IOException {
            if (this.out != null) {
                throw new IOException("Already initialized");
            } else {
                this.out = new DataOutputStream(out);
            }
        }

        /** */
        private static int BUFFER_SIZE = 1024 * 1024;

        /** */
        private ByteBuffer decodeBuffer = ByteBuffer.allocate(BUFFER_SIZE);

        /** */
        public void execute() throws IOException {
            if (out == null) {
                throw new IOException("Not yet initialized");
            } else {
                OpusAudioData nextPacket = in.getNextAudioPacket();
                if (nextPacket != null) {
                    try {
                        int decodedSamples = decoder.decode(nextPacket.getData(), 0, nextPacket.getData().length, decodeBuffer.array(), 0, BUFFER_SIZE, false);
                        if (decodedSamples < 0) {
                            decodeBuffer.clear();
                            out.close();
                            throw new IOException("Decode error");
                        }
                        decodeBuffer.position(decodedSamples * 2 * channels); // 2 bytes per sample
                        decodeBuffer.flip();

                        byte[] decodedData = new byte[decodeBuffer.remaining()];
                        decodeBuffer.get(decodedData);
                        decodeBuffer.flip();

                        out.write(decodedData, 0, decodedData.length);
                    } catch (OpusException e) {
                        out.close();
                        throw new IOException(e);
                    }
                } else {
                    out.close();
                }
            }
        }

        /** */
        public void finish() throws IOException {
            in.close();
        }
    }
}

/* */
