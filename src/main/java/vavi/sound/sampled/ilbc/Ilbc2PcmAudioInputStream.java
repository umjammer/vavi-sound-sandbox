/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.ilbc;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import vavi.io.OutputEngine;
import vavi.io.OutputEngineInputStream;
import vavi.sound.ilbc.Decoder;
import vavi.util.Debug;


/**
 * Converts a iLBC BitStream into a PCM 16bits/sample audio stream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 240704 nsano initial version <br>
 */
class Ilbc2PcmAudioInputStream extends AudioInputStream {

    /**
     * Constructor.
     *
     * @param in     the underlying input stream.
     * @param format the target format of this stream's audio data.
     * @param length the length in sample frames of the data in this stream.
     */
    public Ilbc2PcmAudioInputStream(InputStream in, AudioFormat format, long length) throws IOException {
        super(new OutputEngineInputStream(new IlbcOutputEngine(in)), format, length);
    }

    /** */
    private static class IlbcOutputEngine implements OutputEngine {

        /** */
        private DataOutputStream out;

        /** */
        private final Decoder decoder;

        /** */
        InputStream is;

        /** */
        public IlbcOutputEngine(InputStream is) throws IOException {
            this.is = is;
            decoder = new Decoder(30, 1); // TODO parameter x2
Debug.println(Level.FINE, "iLBC");
            decoded = new byte[decoder.getDecodedLength()];
            buf = new byte[decoder.getEncodedLength()];
        }

        @Override
        public void initialize(OutputStream out) throws IOException {
            if (this.out != null) {
                throw new IOException("Already initialized");
            } else {
                this.out = new DataOutputStream(out);
            }
        }

        byte[] decoded;
        byte[] buf;

        @Override
        public void execute() throws IOException {
            if (out == null) {
                throw new IOException("Not yet initialized");
            } else {
                int r = is.read(buf);
                if (r >= 0) {
                    try {
                        decoder.decode(buf, decoded);

                        out.write(decoded, 0, decoded.length);
                    } catch (IllegalArgumentException e) {
                        out.close();
                        throw new IOException(e);
                    }
                } else {
                    out.close();
                }
            }
        }

        @Override
        public void finish() throws IOException {
        }
    }
}
