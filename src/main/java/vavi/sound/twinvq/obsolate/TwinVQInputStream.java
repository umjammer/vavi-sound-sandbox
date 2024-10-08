/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.twinvq.obsolate;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteOrder;

import vavi.util.Debug;

import static vavi.sound.twinvq.obsolate.TwinVQ.twinVq;


/**
 * TwinVQInputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 070202 initial version <br>
 */
public class TwinVQInputStream extends FilterInputStream {

    /** byte order of the stream obtained with this class */
    private final ByteOrder byteOrder;

    /** */
    public TwinVQInputStream(InputStream in,
                             int samplesPerBlock,
                             int channels,
                             int blockSize)
        throws IOException {

        this(in,
             samplesPerBlock,
             channels,
             blockSize,
             ByteOrder.BIG_ENDIAN);
    }

    /** */
    public TwinVQInputStream(InputStream in,
                             int samplesPerBlock,
                             int channels,
                             int blockSize,
                             ByteOrder byteOrder)
        throws IOException {

        super(new PipedInputStream());

        this.byteOrder = byteOrder;
Debug.println("byteOrder: " + this.byteOrder);

//Debug.println("samplesPerBlock: " + samplesPerBlock);
//Debug.println("channels: " + channels);
//Debug.println("blockSize: " + blockSize);

        //

        InputStream is = new BufferedInputStream(in);

        byte[] packet = new byte[blockSize];
        int[] samples = new int[channels * samplesPerBlock];

        //

        PipedOutputStream pos = new PipedOutputStream((PipedInputStream) this.in);

        Thread thread = new Thread(() -> {

            DataOutputStream os = null;

            try {
                // big endian
                os = new DataOutputStream(pos);

                int done = 0;
                while (done < -1) {

                    int l = 0;
                    while (l < packet.length && is.available() > 0) {
                        l += is.read(packet, l, packet.length - l);
                    }

                    int samplesThisBlock = samplesPerBlock;
//Debug.println("samplesThisBlock: " + samplesThisBlock + ", " + l);

                    TwinVQ.Index index = new TwinVQ.Index();
                    twinVq.TvqDecodeFrame(index , null);

                    for (int i = 0; i < samplesThisBlock; i++) {
                        if (ByteOrder.BIG_ENDIAN.equals(byteOrder)) {
                            os.writeShort(samples[i]);
                        } else {
                            os.write( samples[i] & 0x00ff);
                            os.write((samples[i] & 0xff00) >> 8);
                        }
                    }
                    done += samplesThisBlock;
//Debug.println("done: " + done);
                }
            } catch (IOException e) {
Debug.printStackTrace(e);
            } finally {
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
Debug.println(e);
                }
            }
        });

        thread.start();
    }

    /** */
    private int available;

    @Override
    public int available() throws IOException {
        return available;
    }

    @Override
    public int read() throws IOException {
        available--;
        return in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException("byte[]");
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                 ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException("off: " + off + ", len: " + len);
        } else if (len == 0) {
            return 0;
        }

        int c = read();
        if (c == -1) {
            return -1;
        }
        b[off] = (byte) c;

        int i = 1;
        try {
            for (; i < len ; i++) {
                c = read();
                if (c == -1) {
                    break;
                }
                if (b != null) {
                    b[off + i] = (byte) c;
                }
            }
        } catch (IOException e) {
e.printStackTrace(System.err);
        }
        return i;
    }
}
