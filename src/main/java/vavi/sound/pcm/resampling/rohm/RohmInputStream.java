/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.rohm;

import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import vavi.io.OutputEngine;
import vavi.io.OutputEngineInputStream;
import vavi.util.ByteUtil;


/**
 * RohmInputStream.
 *
 * TODO endian, stereo
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201029 nsano initial version <br>
 */
public class RohmInputStream extends FilterInputStream {

    /** */
    public RohmInputStream(InputStream is, float in, float out) throws IOException {
        super(new OutputEngineInputStream(new RohmOutputEngine(is, in, out)));
    }

    /** */
    private static class RohmOutputEngine implements OutputEngine {

        /** */
        private InputStream in;

        /** */
        private DataOutputStream out;

        /** */
        private Resampler resampler;

        /** */
        public RohmOutputEngine(InputStream is, float in, float out) throws IOException {
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

        /** */
        public void execute() throws IOException {
            if (out == null) {
                throw new IOException("Not yet initialized");
            } else {
                byte[] sample = new byte[0x8192];
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
                    byte[] result = new byte[resamples.length * 2];
                    for (int i = 0; i < resamples.length; i++) {
                        ByteUtil.writeLeShort((short) resamples[i], result, i * 2); // LE
                    }
                    out.write(result, 0, result.length);
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
