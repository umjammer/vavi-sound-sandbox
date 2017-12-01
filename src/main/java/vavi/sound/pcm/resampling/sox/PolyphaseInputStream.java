/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.sox;

import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import vavi.io.OutputEngine;
import vavi.io.OutputEngineInputStream;


/**
 * PolyphaseInputStream.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 111022 nsano initial version <br>
 */
class PolyphaseInputStream extends FilterInputStream {

    /** */
    public PolyphaseInputStream(InputStream is, float in, float out) throws IOException {
        super(new OutputEngineInputStream(new PolyPhaseOutputEngine(is, in, out)));
    }

    /** */
    private static class PolyPhaseOutputEngine implements OutputEngine {

        /** */
        private InputStream in;

        /** */
        private DataOutputStream out;

        /** */
        private Polyphase resampler;

        /** */
        public PolyPhaseOutputEngine(InputStream is, float in, float out) throws IOException {
            this.in = is;
            this.resampler = new Polyphase(in, out);
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
        public void execute() throws IOException {
            if (out == null) {
                throw new IOException("Not yet initialized");
            } else {
                byte[] sample = new byte[0x8192];
                int r = in.read(sample);
                if (r < 0) {
                    out.close();
                } else {
                    int[] samples = new int[r];
                    for (int i = 0; i < r; i++) {
                        samples[i] = sample[i];
                    }
                    int[] result = resampler.resample(samples);  // TODO single channel ???
                    for (int i = 0; i < result.length; i++) { // LE
                        sample[i * 2 + 1] = (byte) (result[i] >>> 8);
                        sample[i * 2] = (byte) result[i];
                    }
                    out.write(sample, 0, result.length);
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
