/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;


/**
 * AVAudioMixerNode.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2020/??/?? nsano initial version <br>
 */
public abstract class AVAudioMixerNode extends AVAudioNode {

    @SuppressWarnings("unused")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioMixerNode", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioMixerNode alloc();
    }

    public abstract AVAudioMixerNode init();

    public static AVAudioMixerNode newInstance() {
        return CLASS.alloc().init();
    }

    public abstract float outputVolume();

    /** The values must be in the range of 0.0 to 1.0. */
    public abstract void setOutputVolume(float vol);
}