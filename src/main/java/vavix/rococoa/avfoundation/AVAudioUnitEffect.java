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

    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioUnitEffect", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioUnitEffect alloc();
    }

    /**
     * synchronous counterpart of {@link AVAudioUnit#instantiate(AudioComponentDescription, int)},
     * which is what we want for in process effects.
     *
     * @param description must exist, a bogus one terminates the jvm (NSException is not catchable)
     * @see AVAudioUnitComponentManager#components(AudioComponentDescription)
     */
    public static AVAudioUnitEffect init(AudioComponentDescription description) {
        AVAudioUnitEffect audioUnit = CLASS.alloc();
        return audioUnit.initWithAudioComponentDescription(description.byValue());
    }

    public abstract AVAudioUnitEffect initWithAudioComponentDescription(AudioComponentDescription.ByValue description);

    public abstract boolean bypass();

    public abstract void setBypass(boolean b);
}