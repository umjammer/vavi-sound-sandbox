
package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;


public abstract class AVAudioUnitEffect extends AVAudioUnit {

    @SuppressWarnings("unused")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioUnitEffect", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioUnitEffect alloc();
    }

    public abstract boolean bypass();

    public abstract void setBypass(boolean b);
}