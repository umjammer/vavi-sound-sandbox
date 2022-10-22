/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mocha;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

import org.junit.jupiter.api.Test;

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

    @Test
    void test1() throws Exception {
        MidiDevice synthesizer = new MochaSynthesizer();
        synthesizer.open();
        Receiver receiver = synthesizer.getReceiver();
Debug.println("receiver: " + receiver);

        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.getTransmitter().setReceiver(receiver);
        sequencer.open();
Debug.println("sequencer: " + sequencer);

//        String filename = "Games/Super Mario Bros 2 - Overworld.mid";
        String filename = "test.mid";

//        File file = new File(System.getProperty("gdrive.home"), "/Music/midi/" + filename);
        Path file = Paths.get("src/test/resources/", filename);

        Sequence seq = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(file)));

        CountDownLatch countDownLatch = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
System.err.println("META: " + meta.getType());
            if (meta.getType() == 47) {
                countDownLatch.countDown();
            }
        };
        sequencer.setSequence(seq);
        sequencer.addMetaEventListener(mel);
System.err.println("START");
        sequencer.start();
if (!System.getProperty("vavi.test", "").equals("ide")) {
 Thread.sleep(10 * 1000);
 sequencer.stop();
 Debug.println("STOP");
} else {
        countDownLatch.await();
}
System.err.println("END");
        sequencer.removeMetaEventListener(mel);
        sequencer.close();

        sequencer.close();
        synthesizer.close();
    }

    // ----

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        MidiDevice synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
        Receiver receiver = synthesizer.getReceiver();
Debug.println("receiver: " + receiver);

        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.getTransmitter().setReceiver(receiver);
        sequencer.open();
Debug.println("sequencer: " + sequencer);

        String filename = "Games/Super Mario Bros 2 - Overworld.mid";

        File file = new File(System.getProperty("gdrive.home"), "/Music/midi/" + filename);
        Sequence seq = MidiSystem.getSequence(file);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
System.err.println("META: " + meta.getType());
            if (meta.getType() == 47) {
                countDownLatch.countDown();
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
