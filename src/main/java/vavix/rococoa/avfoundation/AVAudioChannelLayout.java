/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSObject;


/**
 * AVAudioChannelLayout.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2020/??/?? nsano initial version <br>
 */
public abstract class AVAudioChannelLayout extends NSObject {

    @SuppressWarnings("unused")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioChannelLayout", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioChannelLayout alloc();
    }
}