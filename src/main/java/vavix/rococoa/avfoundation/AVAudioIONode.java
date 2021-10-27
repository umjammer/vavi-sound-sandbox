
package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;

import com.sun.jna.Pointer;


public abstract class AVAudioIONode extends AVAudioNode {

    @SuppressWarnings("hiding")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioIONode", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioIONode alloc();
    }

    /**
     * @return AudioUnit
     */
    public abstract Pointer audioUnit();
}