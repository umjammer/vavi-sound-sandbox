
package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;


public abstract class AVAudioCompressedBuffer extends AVAudioBuffer {

    @SuppressWarnings("hiding")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioCompressedBuffer", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioCompressedBuffer alloc();
    }

    public static AVAudioCompressedBuffer init(AVAudioFormat format, int packetCapacity, int maximumPacketSize) {
        AVAudioCompressedBuffer buffer = CLASS.alloc();
        return buffer.initWithFormat_packetCapacity_maximumPacketSize(format, packetCapacity, maximumPacketSize);
    }

    public abstract AVAudioCompressedBuffer initWithFormat_packetCapacity_maximumPacketSize(AVAudioFormat format,
                                                                                            int packetCapacity,
                                                                                            int maximumPacketSize);
}
