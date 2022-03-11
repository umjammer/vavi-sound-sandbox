
package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSObject;


public abstract class AVAudioBuffer extends NSObject {

    @SuppressWarnings("unused")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioBuffer", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioBuffer alloc();
    }

    public abstract AVAudioFormat format();
}