/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.opl3;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import vavi.util.Debug;


/**
 * Opl3SynthesizerTest.
 *
 * @see "com.sun.media.sound.EmergencySoundbank"
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/05 umjammer initial version <br>
 */
class Opl3SynthesizerTest {

    static {
//        System.setProperty("javax.sound.midi.Sequencer", "#Real Time Sequencer");
        System.setProperty("javax.sound.midi.Synthesizer", "#OPL3 MIDI Synthesizer");
    }

    @Test
    @Disabled
    void test() throws Exception {
        Synthesizer synthesizer = new Opl3Synthesizer();
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);

        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        sequencer.open();
Debug.println("sequencer: " + sequencer);

//        String filename = "1/title-screen.mid";
        String filename = "1/overworld.mid";
//        String filename = "1/m0057_01.mid";
//        String filename = "1/ac4br_gm.MID";
        File file = new File(System.getProperty("user.home"), "/Music/midi/" + filename);
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

        synthesizer.close();
    }

    @Test
    @Disabled
    void test0() throws Exception {
        Synthesizer synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);

        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        sequencer.open();
Debug.println("sequencer: " + sequencer);

//        String filename = "../../src/sano-n/vavi-apps-dx7/tmp/midi/minute_waltz.mid";
//        String filename = "../../src/sano-n/vavi-games-puyo/src/main/resources/sound/puyo_08.mid";
//        String filename = "../../src/sano-n/vavi-apps-dx7/tmp/midi/rachmaninoff-op39-no6.mid";
        String filename = "1/overworld.mid";
        File file = new File(System.getProperty("user.home"), "/Music/midi/" + filename);
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

        synthesizer.close();
    }

    @Test
    void test2() throws Exception {
        Synthesizer synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);

        for (int i = 0; i < 128; i++) {
System.err.println("instrument[" + i + "]");
            synthesizer.getChannels()[0].programChange(i);
            for (int j = 0; j < 12; j++) {
                synthesizer.getChannels()[0].noteOn(50 + j % 12, 127);
                Thread.sleep(200);
                synthesizer.getChannels()[0].noteOff(50 + j % 12);
            }
        }

        synthesizer.close();
    }
}

/* */
