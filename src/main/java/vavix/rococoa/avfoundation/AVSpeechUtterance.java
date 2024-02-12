/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSObject;


/**
 * AVSpeechUtterance.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023/07/01 nsano initial version <br>
 */
public abstract class AVSpeechUtterance extends NSObject {

    @SuppressWarnings("unused")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVSpeechUtterance", _Class.class);

    public interface _Class extends ObjCClass {

        AVSpeechUtterance alloc();

        AVSpeechUtterance speechUtteranceWithString(String string);
    }

    public abstract AVSpeechUtterance initWithString(String string);

    public static AVSpeechUtterance of(String string) {
        return CLASS.speechUtteranceWithString(string);
    }

    public abstract AVSpeechSynthesisVoice voice();

    public abstract void setVoice(AVSpeechSynthesisVoice voice);

    public abstract void setPitchMultiplier(float pitchMultiplier);

    /**
     * Before enqueing the utterance, set this property to a value within the range of 0.0 for silent to 1.0
     * for loudest volume. The default value is 1.0. Setting this after enqueing the utterance has no effect.
     */
    public abstract float volume();

    public abstract void setVolume(float volume);

    public abstract float rate();

    /** 0.0 ~ 0.5 default ~ 1.0 */
    public abstract void setRate(float rate);

    public abstract void setPostUtteranceDelay(float rate);

    public abstract String speechString();

    public abstract String attributedSpeechString();
}