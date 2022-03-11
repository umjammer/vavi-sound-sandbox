/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.opl3;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Opl3SynthesizerTest.
 * <p>
 * {@link Opl3Synthesizer} is separated from OPL3 system.
 * you can play any midi file by this synthesizer.
 * (replay )
 * </p>
 *
 * @see "com.sun.media.sound.EmergencySoundbank"
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/05 umjammer initial version <br>
 */
@DisabledIfEnvironmentVariable(named = "GITHUB_WORKFLOW", matches = ".*")
class Opl3SynthesizerTest {

    static {
//        System.setProperty("javax.sound.midi.Sequencer", "#Real Time Sequencer");
        System.setProperty("javax.sound.midi.Synthesizer", "#OPL3 MIDI Synthesizer");
    }

    static String base;

    @Test
    @DisplayName("direct")
    void test() throws Exception {
        Synthesizer synthesizer = new Opl3Synthesizer();
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);

        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        sequencer.open();
Debug.println("sequencer: " + sequencer);

        String filename = "Games/Super Mario Bros 2 - Overworld.mid";
//        String filename = "Games/リッジレーサー - Theme (GS).mid";

        Path file = Paths.get(System.getProperty("grive.home"), "/Music/midi/", filename);
//        Path file = Paths.get("src/test/resources/", filename);

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
if (Boolean.valueOf(System.getProperty("vavi.test"))) {
 Thread.sleep(10 * 1000);
 sequencer.stop();
 Debug.println("STOP");
} else {
        countDownLatch.await();
}
System.err.println("END");
        sequencer.removeMetaEventListener(mel);
        sequencer.close();

        synthesizer.close();
    }

    @Disabled("not implemented yet")
    @Test
    @DisplayName("spi")
    void test0() throws Exception {
        Synthesizer synthesizer = MidiSystem.getSynthesizer();
        assertEquals(Opl3Synthesizer.class, synthesizer.getClass());
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);

        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        sequencer.open();
Debug.println("sequencer: " + sequencer + ", " + sequencer.getClass().getName());

        String filename = "Games/Super Mario Bros 2 - Overworld.mid";

        Path file = Paths.get(System.getProperty("grive.home"), "/Music/midi/", filename);
//        Path file = Paths.get("src/test/resources/", filename);

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

        int volume = (int) (16383 * .1);
        byte[] data = { (byte) 0xf0, 0x7f, 0x7f, 0x04, 0x01, (byte) (volume & 0x7f), (byte) ((volume >> 7) & 0x7f), (byte) 0xf7 };
        MidiMessage sysex = new SysexMessage(data, data.length);
        Receiver receiver = synthesizer.getReceivers().get(0);
        receiver.send(sysex, -1);

if (Boolean.valueOf(System.getProperty("vavi.test"))) {
 Thread.sleep(10 * 1000);
 sequencer.stop();
 Debug.println("STOP");
} else {
        countDownLatch.await();
}
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

        for (int i = 0; i < 3; i++) {
System.err.println("instrument[" + i + "]");
            int n = new Random().nextInt(128);
            synthesizer.getChannels()[0].programChange(n);
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
