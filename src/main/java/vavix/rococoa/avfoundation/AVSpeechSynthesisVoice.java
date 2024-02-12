/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import java.util.List;

import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSArray;
import org.rococoa.cocoa.foundation.NSObject;


/**
 * AVSpeechSynthesisVoice.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023/07/01 nsano initial version <br>
 */
public abstract class AVSpeechSynthesisVoice extends NSObject {

    @SuppressWarnings("unused")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVSpeechSynthesisVoice", _Class.class);

    public interface _Class extends ObjCClass {
        AVSpeechSynthesisVoice voiceWithLanguage(String languageCode);
        AVSpeechSynthesisVoice voiceWithIdentifier(String identifier);
        NSArray speechVoices();
    }

    public static List<AVSpeechSynthesisVoice> speechVoices() {
        return CLASS.speechVoices().toList();
    }

    public static AVSpeechSynthesisVoice withIdentifier(String identifier) {
        return CLASS.voiceWithIdentifier(identifier);
    }

    public static AVSpeechSynthesisVoice withLanguage(String languageCode) {
        return CLASS.voiceWithLanguage(languageCode);
    }

    public abstract String identifier();
    public abstract String name();

    public static final int AVSpeechSynthesisVoiceGenderUnspecified = 0;
    public static final int AVSpeechSynthesisVoiceGenderMale = 1;
    public static final int AVSpeechSynthesisVoiceGenderFemale = 2;

    public abstract int gender();
}