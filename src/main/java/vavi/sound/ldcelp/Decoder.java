/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.ldcelp;

/**
 * Decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-06-30 nsano initial version <br>
 */
public class Decoder {

    final LdCelp ldCelp = new LdCelp();

    public Decoder(boolean postfilter) {
        ldCelp.postfiltering_p = postfilter;
        ldCelp.init_decoder();
    }

    public void decode(short[] in, short[] out) {
        ldCelp.decoder(in, out);
    }
}
