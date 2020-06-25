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
 * ITLibAlbum.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/02/17 umjammer initial version <br>
 */
public abstract class ITLibAlbum extends NSObject {

    @SuppressWarnings("unused")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("ITLibAlbum", _Class.class);

    interface _Class extends ObjCClass {
        ITLibAlbum alloc();
    }

    // NullAllowed
    public abstract String title();

    // NullAllowed
    public abstract String sortTitle();

    public abstract boolean isCompilation();

    // NullAllowed
    public abstract ITLibArtist artist();

    public abstract int discCount();

    public abstract int discNumber();

    public abstract int rating();

    public abstract boolean isRatingComputed();

    public abstract boolean isGapless();

    public abstract int trackCount();

    // NullAllowed
    public abstract String albumArtist();

    // NullAllowed
    public abstract String sortAlbumArtist();

    public abstract NSNumber persistentID();
}
