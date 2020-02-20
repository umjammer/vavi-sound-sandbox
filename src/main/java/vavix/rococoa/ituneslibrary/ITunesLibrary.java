/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.ituneslibrary;


/**
 * ITunesLibrary.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/02/17 umjammer initial version <br>
 * @see "https://github.com/dpogue/Unofficial-Google-Music-API/wiki/Skyjam-API"
 */
public interface ITunesLibrary extends com.sun.jna.Library {
    public static ITunesLibrary instance = (ITunesLibrary) com.sun.jna.Native.loadLibrary("iTunesLibrary", ITunesLibrary.class);
}

//delegate void ITLibMediaEntityEnumerateValuesHandler (NSString property, NSObject value, out bool stop);

/* */
