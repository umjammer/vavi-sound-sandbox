/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.rococoa;

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
 * RococoaMidiDeviceProviderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/03 umjammer initial version <br>
 */
class RococoaSynthesizerTest {

    static {
//        System.setProperty("vavi.sound.midi.rococoa.RococoaSynthesizer.audesc", "DGSB:Dexd");
//        System.setProperty("vavi.sound.midi.rococoa.RococoaSynthesizer.audesc", "AKai:MpcB");
//        System.setProperty("vavi.sound.midi.rococoa.RococoaSynthesizer.audesc", "NiSc:nK1v");
//        System.setProperty("vavi.sound.midi.rococoa.RococoaSynthesizer.audesc", "Ftcr:mc5p");
        System.setProperty("javax.sound.midi.Sequencer", "#Real Time Sequencer");
        System.setProperty("javax.sound.midi.Synthesizer", "Rococoa MIDI Synthesizer");
    }

    @Test
    void test() throws Exception {
        Synthesizer synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);

        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        sequencer.open();
Debug.println("sequencer: " + sequencer);

//        String filename = "1/風の谷のナウシカ メインテーマ.mid";
        String filename = "1/ac4br_gm.MID";
//        String filename = "1/m0057_01.mid";
//        String filename = "town.mid";
        File file = new File(System.getProperty("user.home"), "Music/midi/" + filename);
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
    void test1() throws Exception {
        Synthesizer synthesizer = new RococoaSynthesizer();
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);

        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        sequencer.open();
Debug.println("sequencer: " + sequencer);

//        String filename = "ac4br_s.MID";
        String filename = "ac4br_gm.MID";
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
}

/* */
