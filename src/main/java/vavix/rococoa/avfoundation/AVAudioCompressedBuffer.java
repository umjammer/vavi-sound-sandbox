/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;


/**
 * AVAudioCompressedBuffer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2020/??/?? nsano initial version <br>
 */
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
