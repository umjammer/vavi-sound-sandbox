
package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSObject;


public abstract class AVAudioNode extends NSObject {

    @SuppressWarnings("unused")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioNode", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioNode alloc();
    }

    public abstract AVAudioFormat inputFormatForBus(int bus);

    public abstract String nameForInputBus(int bus);

    public abstract int numberOfInputs();

    public abstract AVAudioFormat outputFormatForBus(int bus);

    public abstract String nameForOutputBus(int bus);

    public abstract int numberOfOutputs();

    public abstract AVAudioEngine engine();

    public abstract void reset();

    public abstract AUAudioUnit AUAudioUnit();

    public abstract double latency();

    public abstract double outputPresentationLatency();
}