/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;


/**
 * AVFoundation.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/02 umjammer initial version <br>
 */
public interface AVFoundation extends com.sun.jna.Library {
    public static AVFoundation instance = com.sun.jna.Native.load("AVFoundation", AVFoundation.class);
}

/* */
