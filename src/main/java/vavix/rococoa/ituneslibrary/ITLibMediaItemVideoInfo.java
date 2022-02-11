/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.ituneslibrary;

import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSObject;


/**
 * ITLibMediaItemVideoInfo.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/02/17 umjammer initial version <br>
 */
public abstract class ITLibMediaItemVideoInfo extends NSObject {

    @SuppressWarnings("unused")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("ITLibMediaItemVideoInfo", _Class.class);

    interface _Class extends ObjCClass {
        ITLibMediaItemVideoInfo alloc();
    }

    // NullAllowed
    public abstract String series();

    // NullAllowed
    public abstract String sortSeries();

    public abstract int season();

    // NullAllowed
    public abstract String episode();

    public abstract int episodeOrder();

    public abstract boolean isHD();

    public abstract int videoWidth();

    public abstract int videoHeight();
}
