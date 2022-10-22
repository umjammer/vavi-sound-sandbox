/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.equalizing.sse;


/**
 * ClipTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060419 nsano initial version <br>
 */
public class Test1 {

    /**
     */
    public static void main(String[] args) {
        for (int i = 0; i < 97; i++) {
            System.err.println(Math.pow(10, i / -20.0));
        }
    }
}

/* */
