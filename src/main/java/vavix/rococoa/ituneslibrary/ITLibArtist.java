/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.ituneslibrary;

import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSNumber;
import org.rococoa.cocoa.foundation.NSObject;


/**
 * ITLibArtist.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/02/17 umjammer initial version <br>
 */
public abstract class ITLibArtist extends NSObject {

    @SuppressWarnings("hiding")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("ITLibArtist", _Class.class);

    interface _Class extends ObjCClass {
        ITLibArtist alloc();
    }

    // NullAllowed
    public abstract String name();

    // NullAllowed
    public abstract String sortName();

    public abstract NSNumber persistentID();
}
