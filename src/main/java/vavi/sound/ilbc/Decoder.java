/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.ilbc;

import java.io.IOException;


/**
 * Decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-07-04 nsano initial version <br>
 */
public class Decoder {

    private final Ilbc.Decoder decoder = new Ilbc.Decoder();

    public Decoder(int mode, int useEnhancer) {
        Ilbc.initDecode(decoder, mode, useEnhancer);
    }

    public int getDecodedLength() {
        return (decoder.mode == 30 ? Ilbc.BLOCKL_30MS : Ilbc.BLOCKL_20MS) * Short.BYTES;
    }

    public int getEncodedLength() {
        return decoder.mode == 30 ? Ilbc.NO_OF_BYTES_30MS : Ilbc.NO_OF_BYTES_20MS;
    }

    /** */
    public void decode(byte[] encoded_data, byte[] decoded_data) throws IOException {
        Ilbc.decode(decoder, decoded_data, encoded_data, 1);
    }
}
