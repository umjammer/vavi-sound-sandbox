
package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSDictionary;
import org.rococoa.cocoa.foundation.NSObject;

public abstract class AVAudioFormat extends NSObject {

    enum AVAudioCommonFormat {
        otherFormat,
        pcmFormatFloat32,
        pcmFormatFloat64,
        pcmFormatInt16,
        pcmFormatInt32
    }

    @SuppressWarnings("hiding")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioFormat", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioFormat alloc();
    }

    public static AVAudioFormat init(AVAudioCommonFormat format, double sampleRate, int channels, boolean interleaved) {
        AVAudioFormat audioFormat = CLASS.alloc();
        return audioFormat.initWithCommonFormat_sampleRate_channels_interleaved(format.ordinal(), sampleRate, channels, interleaved);
    }

    // TODO not work
    public static AVAudioFormat init(AudioStreamBasicDescription streamDescription) {
        AVAudioFormat audioFormat = CLASS.alloc();
        return audioFormat.initWithStreamDescription(streamDescription);
    }

    public static AVAudioFormat init(NSDictionary settings) {
        AVAudioFormat audioFormat = CLASS.alloc();
        return audioFormat.initWithSettings(settings);
    }

    public abstract AVAudioFormat initWithCommonFormat_sampleRate_channels_interleaved(int outBuffer, double sampleRate, int channels, boolean interleaved);

    public abstract AVAudioFormat initWithStreamDescription(AudioStreamBasicDescription streamDescription);

    public abstract AVAudioFormat initWithSettings(NSDictionary settings);

    public abstract double sampleRate();

    public abstract int channelCount();

    public abstract int commonFormat();

    public static final int OtherFormat = 0;
    public static final int PCMFormatFloat32 = 1;
    public static final int PCMFormatFloat64 = 2;
    public static final int PCMFormatInt16 = 3;
    public static final int PCMFormatInt32 = 4;

    public abstract NSDictionary settings();
}