/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import com.sun.jna.Pointer;

import org.rococoa.ObjCClass;


/**
 * @interface AVAudioPCMBuffer : AVAudioBuffer
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/??/?? umjammer initial version <br>
 */
public abstract class AVAudioPCMBuffer extends AVAudioBuffer {

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

    public abstract Pointer floatChannelData();

    public abstract Pointer int16ChannelData();

    public abstract Pointer int32ChannelData();

    public abstract /* AVAudioFrameCount */ int frameCapacity();

    public abstract /* NSUInteger */ int stride();
}
