
package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSObject;


public abstract class AVAudioNode extends NSObject {

    @SuppressWarnings("hiding")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioNode", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioNode alloc();
    }

    public abstract AVAudioEngine engine();

    public abstract void reset();
}