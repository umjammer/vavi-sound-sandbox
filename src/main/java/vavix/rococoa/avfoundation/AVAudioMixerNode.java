
package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;


public abstract class AVAudioMixerNode extends AVAudioNode {

    @SuppressWarnings("unused")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioMixerNode", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioMixerNode alloc();
    }

    public abstract AVAudioMixerNode init();

    public abstract float outputVolume();

    /** The values must be in the range of 0.0 to 1.0. */
    public abstract void outputVolume(float vol);
}