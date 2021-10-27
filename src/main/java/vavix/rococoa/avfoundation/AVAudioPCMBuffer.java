
package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;


public abstract class AVAudioPCMBuffer extends AVAudioBuffer {

    @SuppressWarnings("hiding")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioPCMBuffer", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioPCMBuffer alloc();
    }

    public static AVAudioPCMBuffer init(AVAudioFormat format, int frameCapacity) {
        AVAudioPCMBuffer buffer = CLASS.alloc();
        return buffer.initWithPCMFormat_frameCapacity(format, frameCapacity);
    }

    public abstract AVAudioPCMBuffer initWithPCMFormat_frameCapacity(AVAudioFormat format, int frameCapacity);

    public abstract int frameLength();
}
