/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import vavi.util.Debug;
import vavix.rococoa.avfoundation.AVSpeechSynthesizer.AVSpeechSynthesizerBufferCallback;

import static org.rococoa.ObjCBlocks.block;


/**
 * SpeechTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-07-01 nsano initial version <br>
 */
public class SpeechTest {

    @Test
    @DisabledIfSystemProperty(named = "os.arch", matches = "x86_64")
    void test1() throws Exception {
//        for (NSObject voice : AVSpeechSynthesisVoice.speechVoices()) { // TODO cannot use AVSpeechSynthesisVoice instead of NSObject
//            System.err.println(voice);
//        }

        CountDownLatch cdl = new CountDownLatch(1);

        AVSpeechSynthesizer synthesizer = AVSpeechSynthesizer.newInstance();
        synthesizer.setDelegate(new AVSpeechSynthesizer.AVSpeechSynthesizerAdapter() {
            @Override public void speechSynthesizer_didFinishSpeechUtterance(AVSpeechSynthesizer sender, AVSpeechUtterance utterance) {
                cdl.countDown();
            }
        });

        AVSpeechSynthesisVoice voice = AVSpeechSynthesisVoice.withLanguage("en-US");
Debug.println("voice: " + voice);

        AVSpeechUtterance utterance = AVSpeechUtterance.of("she sells seashells by the seashore");
        utterance.setVoice(voice);
        utterance.setVolume(.2f);
        utterance.setRate(.5f);
        utterance.setPitchMultiplier(1.0f);

        synthesizer.speakUtterance(utterance);

        cdl.await();
    }

    @Test
    @Disabled("crash")
    @DisabledIfSystemProperty(named = "os.arch", matches = "x86_64")
    void test2() throws Exception {
        CountDownLatch cdl = new CountDownLatch(1);

        AVSpeechSynthesizer synthesizer = AVSpeechSynthesizer.newInstance();
        synthesizer.setDelegate(new AVSpeechSynthesizer.AVSpeechSynthesizerAdapter() {
            @Override public void speechSynthesizer_didFinishSpeechUtterance(AVSpeechSynthesizer sender, AVSpeechUtterance utterance) {
                cdl.countDown();
            }
        });

        AVSpeechSynthesisVoice voice = AVSpeechSynthesisVoice.withLanguage("en-US");
Debug.println("voice: " + voice);

        AVSpeechUtterance utterance = AVSpeechUtterance.of("she sells seashells by the seashore");
        utterance.setVoice(voice);
        utterance.setVolume(.2f);
        utterance.setRate(.5f);
        utterance.setPitchMultiplier(1.0f);

        synthesizer.writeUtterance_toBufferCallback(utterance, block((AVSpeechSynthesizerBufferCallback)((literal, buffer) -> {
//            Debug.println("here!");
        })));

        cdl.await();
    }
}
