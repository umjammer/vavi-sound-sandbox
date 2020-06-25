/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.ituneslibrary;

import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSArray;


public abstract class ITLibPlaylist extends ITLibMediaEntity {

    @SuppressWarnings("unused")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("ITLibPlaylist", _Class.class);

    interface _Class extends ObjCClass {
        ITLibPlaylist alloc();
    }

//    public abstract String name(); // TODO error

//    public abstract boolean isMaster();

    // @Nullable
//    public abstract NSNumber parentID();

//    public abstract boolean isVisible();

//    public abstract boolean isAllItemsPlaylist();

    /**
     * @return ITLibMediaItem
     */
    public abstract NSArray items();

//    public abstract /*ITLibDistinguishedPlaylistKind*/int distinguishedKind();

//    public abstract /*ITLibPlaylistKind*/int kind();
}
