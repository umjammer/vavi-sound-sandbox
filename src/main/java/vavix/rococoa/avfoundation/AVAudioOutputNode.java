/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;


/**
 * @interface AVAudioOutputNode : AVAudioIONode
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/??/?? umjammer initial version <br>
 */
public abstract class AVAudioOutputNode extends AVAudioIONode {

    @SuppressWarnings("unused")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioOutputNode", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioOutputNode alloc();
    }
}