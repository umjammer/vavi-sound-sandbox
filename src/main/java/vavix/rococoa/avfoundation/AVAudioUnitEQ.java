
package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;


public abstract class AVAudioUnitEQ extends AVAudioUnitEffect {

    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioUnitEQ", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioUnitEQ alloc();
    }

    public static AVAudioUnitEQ newInstance() {
        return CLASS.alloc().init();
    }

    public static AVAudioUnitEQ newInstance(int numberOfBands) {
        return CLASS.alloc().initWithNumberOfBands(numberOfBands);
    }

    public abstract AVAudioUnitEQ init();

    public abstract AVAudioUnitEQ initWithNumberOfBands(int numberOfBands);

    /**
     * The overall gain adjustment that the audio unit applies to the signal, in decibels.
     */
    public abstract float globalGain();

    /**
     * The default value is 0 db. The valid range of values is -96 db to 24 db.
     */
    public abstract void setGlobalGain(float value);
}