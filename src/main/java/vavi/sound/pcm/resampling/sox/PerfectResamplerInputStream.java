/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.sox;

import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.io.OutputEngine;
import vavi.io.OutputEngineInputStream;
import vavi.util.ByteUtil;

import static java.lang.System.getLogger;


/**
 * PerfectResamplerInputStream.
 *
 * Wraps {@link PerfectResampler} (SoX `rate` "Perfect Resampler") as a 16bit LE
 * mono PCM {@link InputStream} filter, following the same flow/drain protocol
 * used by {@code PerfectResamplerTest}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 250707 nsano initial version <br>
 * @see PerfectResampler
 */
public class PerfectResamplerInputStream extends FilterInputStream {

    private static final Logger logger = getLogger(PerfectResamplerInputStream.class.getName());

    /** */
    public PerfectResamplerInputStream(InputStream is, float in, float out) throws IOException {
        super(new OutputEngineInputStream(new PerfectResamplerOutputEngine(is, in, out)));
    }

    /** */
    private static class PerfectResamplerOutputEngine implements OutputEngine {

        /** */
        private final InputStream in;

        /** */
        private DataOutputStream out;

        /** */
        private final PerfectResampler resampler;

        /** working output buffer, drained fully every call */
        private final int[] obuf = new int[65536];

        /** */
        public PerfectResamplerOutputEngine(InputStream is, float in, float out) throws IOException {
            this.in = is;
            // coef_interp=2, phase=25 (intermediate), no bandwidth override,
            // no aliasing, quality=Default(-1) -> high
            this.resampler = new PerfectResampler(2, 25, 0, false, -1, in, out);
        }

        @Override
        public void initialize(OutputStream out) throws IOException {
            if (this.out != null) {
                throw new IOException("Already initialized");
            } else {
                this.out = new DataOutputStream(out);
            }
        }

        private final byte[] sample = new byte[8192 * 2];

        @Override
        public void execute() throws IOException {
            if (out == null) {
                throw new IOException("Not yet initialized");
            } else {
                int r = in.read(sample);
                if (r < 0) {
                    // drain remaining buffered output
                    int[] osamp = { obuf.length };
                    do {
                        osamp[0] = obuf.length;
                        resampler.drain(obuf, osamp);
                        out.write(toBytes(obuf, osamp[0]));
                    } while (osamp[0] > 0);
                    out.flush();
                    out.close();
                } else {
                    int[] samples = new int[r / 2];
                    for (int i = 0; i < samples.length; i++) {
                        samples[i] = ByteUtil.readLeShort(sample, i * 2); // LE
                    }
                    // feed this chunk (emits some previously-produced output)
                    int[] isamp = { samples.length };
                    int[] osamp = { obuf.length };
                    resampler.flow(samples, obuf, isamp, osamp);
                    out.write(toBytes(obuf, osamp[0]));
                    // pump the rest that is now ready without feeding new input
                    do {
                        isamp[0] = 0;
                        osamp[0] = obuf.length;
                        resampler.flow(null, obuf, isamp, osamp);
                        out.write(toBytes(obuf, osamp[0]));
                    } while (osamp[0] > 0);
logger.log(Level.DEBUG, r / 2 + " in");
                }
            }
        }

        /** 16bit LE */
        private static byte[] toBytes(int[] samples, int length) {
            byte[] result = new byte[length * 2];
            for (int i = 0; i < length; i++) {
                int v = Math.clamp(samples[i], -32768, 32767);
                ByteUtil.writeLeShort((short) v, result, i * 2); // LE
            }
            return result;
        }

        @Override
        public void finish() throws IOException {
            in.close();
        }
    }
}
