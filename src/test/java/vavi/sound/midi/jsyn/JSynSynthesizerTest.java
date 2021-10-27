/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.jsyn;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import vavi.util.Debug;


/**
 * JSynSynthesizerTest.
 *
 * TODO really plays polyphony?
 *
 * @see "com.sun.media.sound.EmergencySoundbank"
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/05 umjammer initial version <br>
 */
class JSynSynthesizerTest {

    static {
        System.setProperty("javax.sound.midi.Sequencer", "#Real Time Sequencer");
//        System.setProperty("javax.sound.midi.Synthesizer", "#JSyn MIDI Synthesizer");
        System.setProperty("javax.sound.midi.Synthesizer", "#Gervill");
    }

    @Test
    void test() throws Exception {
        Synthesizer synthesizer = new JSynSynthesizer();
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);

        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        sequencer.open();
Debug.println("sequencer: " + sequencer);

//        String filename = "../../../src/sano-n/vavi-apps-dx7/tmp/midi/minute_waltz.mid";
//        String filename = "title-screen.mid";
        String filename = "overworld.mid";
//        String filename = "m0057_01.mid";
//        String filename = "ac4br_gm.MID";
        File file = new File(System.getProperty("user.home"), "/Music/midi/1/" + filename);
        Sequence seq = MidiSystem.getSequence(file);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        MetaEventListener mel = new MetaEventListener() {
            public void meta(MetaMessage meta) {
System.err.println("META: " + meta.getType());
                if (meta.getType() == 47) {
                    countDownLatch.countDown();
                }
            }
        };
        sequencer.setSequence(seq);
        sequencer.addMetaEventListener(mel);
System.err.println("START");
        sequencer.start();
        countDownLatch.await();
System.err.println("END");
        sequencer.removeMetaEventListener(mel);
        sequencer.close();

        sequencer.close();
        synthesizer.close();
    }

    @Test
    @Disabled
    void test3() throws Exception {
        Synthesizer synthesizer = MidiSystem.getSynthesizer();
Debug.println("synthesizer: " + synthesizer.getClass().getName());
        synthesizer.open();
        synthesizer.unloadAllInstruments(synthesizer.getDefaultSoundbank());
        synthesizer.loadAllInstruments(new JSynOscillator());

//        String filename = "../../src/sano-n/vavi-apps-dx7/tmp/midi/minute_waltz.mid";
//        String filename = "1/title-screen.mid";
        String filename = "1/overworld.mid";
//        String filename = "1/m0057_01.mid";
//        String filename = "1/ac4br_gm.MID";
        File file = new File(System.getProperty("user.home"), "/Music/midi/" + filename);
        Sequence seq = MidiSystem.getSequence(file);

        Sequencer sequencer = MidiSystem.getSequencer(false);
Debug.println("sequencer: " + sequencer.getClass().getName());
        sequencer.open();
        Receiver receiver = synthesizer.getReceiver();
        sequencer.getTransmitter().setReceiver(receiver);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        MetaEventListener mel = new MetaEventListener() {
            public void meta(MetaMessage meta) {
System.err.println("META: " + meta.getType());
                if (meta.getType() == 47) {
                    countDownLatch.countDown();
                }
            }
        };
        sequencer.setSequence(seq);
        sequencer.addMetaEventListener(mel);
System.err.println("START");
        sequencer.start();

        countDownLatch.await();
System.err.println("END");
        sequencer.removeMetaEventListener(mel);

        sequencer.stop();
        sequencer.close();
        synthesizer.close();
    }
}

/* */
