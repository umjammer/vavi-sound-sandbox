/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import org.rococoa.ID;
import org.rococoa.ObjCClass;
import org.rococoa.ObjCObject;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSObject;
import org.rococoa.cocoa.foundation.NSRange;


/**
 * AVSpeechSynthesizer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023/07/01 nsano initial version <br>
 */
public abstract class AVSpeechSynthesizer extends NSObject {

    @SuppressWarnings("unused")
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVSpeechSynthesizer", _Class.class);

    public interface _Class extends ObjCClass {
        AVSpeechSynthesizer alloc();
    }

    public abstract AVSpeechSynthesizer init();

    public static AVSpeechSynthesizer newInstance() {
        return CLASS.alloc().init();
    }

    public abstract void speakUtterance(AVSpeechUtterance utterance);
    public abstract boolean isSpeaking();
    public abstract boolean isPaused();

    public abstract boolean continueSpeaking();

    public static final int AVSpeechBoundaryImmediate = 0;
    public static final int AVSpeechBoundaryWord = 1;

    public abstract boolean pauseSpeakingAtBoundary(int boundary);
    public abstract boolean stopSpeakingAtBoundary(int boundary);

    public interface AVSpeechSynthesizerDelegate {
        void speechSynthesizer_didStartSpeechUtterance(AVSpeechSynthesizer synthesizer, AVSpeechUtterance utterance);

        void speechSynthesizer_willSpeakRangeOfSpeechString_utterance(AVSpeechSynthesizer sender, NSRange characterRange, AVSpeechUtterance utterance);

        void speechSynthesizer_didPauseSpeechUtterance(AVSpeechSynthesizer sender, AVSpeechUtterance utterance);

        void speechSynthesizer_didContinueSpeechUtterance(AVSpeechSynthesizer sender, AVSpeechUtterance utterance);

        void speechSynthesizer_didFinishSpeechUtterance(AVSpeechSynthesizer sender, AVSpeechUtterance utterance);

        void speechSynthesizer_didCancelSpeechUtterance(AVSpeechSynthesizer sender, AVSpeechUtterance utterance);
    }

    public static class AVSpeechSynthesizerAdapter implements AVSpeechSynthesizerDelegate {
        @Override public void speechSynthesizer_didStartSpeechUtterance(AVSpeechSynthesizer synthesizer, AVSpeechUtterance utterance) {}

        @Override public void speechSynthesizer_willSpeakRangeOfSpeechString_utterance(AVSpeechSynthesizer sender, NSRange characterRange, AVSpeechUtterance utterance) {}

        @Override public void speechSynthesizer_didPauseSpeechUtterance(AVSpeechSynthesizer sender, AVSpeechUtterance utterance) {}

        @Override public void speechSynthesizer_didContinueSpeechUtterance(AVSpeechSynthesizer sender, AVSpeechUtterance utterance) {}

        @Override public void speechSynthesizer_didFinishSpeechUtterance(AVSpeechSynthesizer sender, AVSpeechUtterance utterance) {}

        @Override public void speechSynthesizer_didCancelSpeechUtterance(AVSpeechSynthesizer sender, AVSpeechUtterance utterance) {}
    }

    public abstract void setDelegate(ID delegate);

    public synchronized void setDelegate(AVSpeechSynthesizerDelegate delegate) {
        ObjCObject delegateProxy = Rococoa.proxy(delegate);
        setDelegate(delegateProxy.id());
    }

    public abstract void writeUtterance_toBufferCallback(AVSpeechUtterance utterance, ID bufferCallback);
}