/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.twinvq;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;

import vavi.io.LittleEndianDataInputStream;
import vavi.util.Debug;


/**
 * TwinVQOutputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 070202 initial version <br>
 */
public class TwinVQOutputStream extends FilterOutputStream {

    /** */
    private final ByteOrder byteOrder;

    /**
     */
    public TwinVQOutputStream(OutputStream out)
        throws IOException {

        this(out, ByteOrder.LITTLE_ENDIAN);
    }

    /** */
    private final OutputStream realOut;

    /**
     */
    public TwinVQOutputStream(OutputStream out, ByteOrder byteOrder)
        throws IOException {

        super(new ByteArrayOutputStream());

        this.byteOrder = byteOrder;
Debug.println("byteOrder: " + this.byteOrder);

        realOut = out;
    }

    /**
     * 必ず呼んでね。
     */
    public void close() throws IOException {

        TwinVQ encoder = TwinVQ.getInstance();

        try {
            LittleEndianDataInputStream ledis = new LittleEndianDataInputStream(new ByteArrayInputStream(((ByteArrayOutputStream) out).toByteArray()));
            int length = ledis.available();
Debug.println("length: " + length);
            byte[] adpcm = new byte[length / 4];
            int[] pcm = new int[length / 2];
            for (int i = 0; i < pcm.length; i++) {
                pcm[i] = ledis.readShort();
            }
            ledis.close();

            TwinVQ.Index i = new TwinVQ.Index();
            encoder.TvqEncodeFrame(null, i);

            realOut.write(adpcm);

        } catch (IOException e) {
Debug.printStackTrace(e);
        } finally {
            try {
                realOut.flush();
                realOut.close();
            } catch (IOException e) {
Debug.println(e);
            }
        }

        realOut.close();
    }
}

/* */
