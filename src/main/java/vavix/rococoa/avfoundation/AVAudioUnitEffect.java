/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;


/**
 * @interface AVAudioUnitEffect : AVAudioUnit
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/??/?? umjammer initial version <br>
 */
public abstract class AVAudioUnitEffect extends AVAudioUnit {

    @SuppressWarnings("unused")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioUnitEffect", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioUnitEffect alloc();
    }

    public abstract boolean bypass();

    public abstract void setBypass(boolean b);
}