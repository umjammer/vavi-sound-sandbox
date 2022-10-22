/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.jsyn;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;

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

    static String base;

    @Test
    void test() throws Exception {
        Synthesizer synthesizer = new JSynSynthesizer();
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);

        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        sequencer.open();
Debug.println("sequencer: " + sequencer);

//        String filename = "Games/Super Mario Bros 2 - Overworld.mid";
//        String filename = "/Fusion/YMO - Firecracker.mid";
        String filename = "test.mid";

//        Path file = Paths.get(System.getProperty("gdrive.home"), "/Music/midi/", filename);
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

        int volume = (int) (16383 * .2);
        byte[] data = { (byte) 0xf0, 0x7f, 0x7f, 0x04, 0x01, (byte) (volume & 0x7f), (byte) ((volume >> 7) & 0x7f), (byte) 0xf7 };
        MidiMessage sysex = new SysexMessage(data, data.length);
        Receiver receiver = synthesizer.getReceivers().get(0);
        receiver.send(sysex, -1);

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

    @Test
    void test3() throws Exception {
        Synthesizer synthesizer = MidiSystem.getSynthesizer();
Debug.println("synthesizer: " + synthesizer.getClass().getName());
        synthesizer.open();
        synthesizer.unloadAllInstruments(synthesizer.getDefaultSoundbank());
        synthesizer.loadAllInstruments(new JSynOscillator());

//        String filename = "Games/Super Mario Bros 2 - Overworld.mid";
        String filename = "test.mid";

//        Path file = Paths.get(System.getProperty("gdrive.home"), "/Music/midi/", filename);
        Path file = Paths.get("src/test/resources/", filename);

        Sequence seq = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(file)));

        Sequencer sequencer = MidiSystem.getSequencer(false);
Debug.println("sequencer: " + sequencer.getClass().getName());
        sequencer.open();
        Receiver receiver = synthesizer.getReceiver();
        sequencer.getTransmitter().setReceiver(receiver);

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

        sequencer.stop();
        sequencer.close();
        synthesizer.close();
    }
}

/* */
