/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.ldclep;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import vavi.io.OutputEngine;
import vavi.io.OutputEngineInputStream;
import vavi.sound.ldcelp.Decoder;
import vavi.util.Debug;


/**
 * Converts a LD-CELP BitStream into a PCM 16bits/sample audio stream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 240629 nsano initial version <br>
 */
class LdCelp2PcmAudioInputStream extends AudioInputStream {

    /**
     * Constructor.
     *
     * @param in     the underlying input stream.
     * @param format the target format of this stream's audio data.
     * @param length the length in sample frames of the data in this stream.
     */
    public LdCelp2PcmAudioInputStream(InputStream in, AudioFormat format, long length) throws IOException {
        super(new OutputEngineInputStream(new LdCelpOutputEngine(in)), format, length);
    }

    /** */
    private static class LdCelpOutputEngine implements OutputEngine {

        /** */
        private DataOutputStream out;

        /** */
        private final Decoder decoder;

        /** */
        InputStream is;

        /** */
        public LdCelpOutputEngine(InputStream is) throws IOException {
            this.is = is;
            decoder = new Decoder(true); // TODO parameter postfilter
Debug.println(Level.FINE, "LD-CELP");
        }

        @Override
        public void initialize(OutputStream out) throws IOException {
            if (this.out != null) {
                throw new IOException("Already initialized");
            } else {
                this.out = new DataOutputStream(out);
            }
        }

        byte[] buf = new byte[Short.BYTES];
        byte[] decoded = new byte[5 * Short.BYTES];

        short[] inDataS = new short[1];
        ShortBuffer isb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        short[] outDataS = new short[5];
        ShortBuffer osb = ByteBuffer.wrap(decoded).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();

        @Override
        public void execute() throws IOException {
            if (out == null) {
                throw new IOException("Not yet initialized");
            } else {
                int r = is.read(buf);
                if (r >= 0) {
                    try {
                        isb.get(inDataS);
                        isb.flip();
                        decoder.decode(inDataS, outDataS);
                        osb.put(outDataS);
                        osb.flip();

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
