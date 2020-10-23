
package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;


public abstract class AVAudioMixerNode extends AVAudioNode {

    @SuppressWarnings("hiding")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioMixerNode", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioMixerNode alloc();
    }

    public abstract AVAudioMixerNode init();
}