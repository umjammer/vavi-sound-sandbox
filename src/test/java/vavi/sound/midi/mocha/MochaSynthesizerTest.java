/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mocha;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

import vavi.util.Debug;


/**
 * MochaSynthesizer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/30 umjammer initial version <br>
 */
public class MochaSynthesizerTest {

    static {
        System.setProperty("javax.sound.midi.Sequencer", "#Real Time Sequencer");
        System.setProperty("javax.sound.midi.Synthesizer", "#Mocha MIDI Synthesizer");
    }

    public static void main(String[] args) throws Exception {
        MidiDevice synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
        Receiver receiver = synthesizer.getReceiver();
Debug.println("receiver: " + receiver);

        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.getTransmitter().setReceiver(receiver);
        sequencer.open();
Debug.println("sequencer: " + sequencer);

        String filename = "m0057_01.mid";
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
}
