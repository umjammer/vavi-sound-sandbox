/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
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
import vavi.util.ByteUtil;
import vavi.util.Debug;


/**
 * ResamplerInputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201029 nsano initial version <br>
 */
class ResamplerInputStream extends FilterInputStream {

    /** */
    public ResamplerInputStream(InputStream is, float in, float out) throws IOException {
        super(new OutputEngineInputStream(new ResamplerOutputEngine(is, in, out)));
    }

    /** */
    private static class ResamplerOutputEngine implements OutputEngine {

        /** */
        private final InputStream in;

        /** */
        private DataOutputStream out;

        /** */
        private final Resampler resampler;

        /** */
        public ResamplerOutputEngine(InputStream is, float in, float out) throws IOException {
            this.in = is;
            this.resampler = new Resampler(in, out);
        }

        /** */
        public void initialize(OutputStream out) throws IOException {
            if (this.out != null) {
                throw new IOException("Already initialized");
            } else {
                this.out = new DataOutputStream(out);
            }
        }

        private final byte[] sample = new byte[44100 * 2];

        /** */
        public void execute() throws IOException {
            if (out == null) {
                throw new IOException("Not yet initialized");
            } else {
                int r = in.read(sample);
                if (r < 0) {
                    out.flush();
                    out.close();
                } else {
                    int[] samples = new int[r / 2];
                    for (int i = 0; i < samples.length; i++) {
                        samples[i] = ByteUtil.readLeShort(sample, i * 2); // LE
                    }
                    int[] resamples = resampler.resample(samples);  // TODO single channel ???
Debug.println(r / 2 + ", " + resamples.length);
                    byte[] result = new byte[resamples.length * 2];
                    for (int i = 0; i < resamples.length; i++) {
                        ByteUtil.writeLeShort((short) resamples[i], result, i * 2); // LE
                    }
                    out.write(result);
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
