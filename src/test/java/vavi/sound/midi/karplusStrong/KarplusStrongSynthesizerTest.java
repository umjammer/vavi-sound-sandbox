/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.karplusStrong;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.sound.midi.MidiUtil.volume;


/**
 * KarplusStrongSynthesizerTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2026/07/04 umjammer initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class KarplusStrongSynthesizerTest {

    static {
        System.setProperty("javax.sound.midi.Sequencer", "#Real Time Sequencer");

        System.setProperty("javax.sound.midi.Synthesizer", "#KarplusStrong MIDI Synthesizer");
    }

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    static boolean onIde = System.getProperty("vavi.test", "").equals("ide");
    static long time = onIde ? 1000 * 1000 : 10 * 1000;

    @Property(name = "vavi.test.volume.midi")
    float volume = 0.2f;

    @Property(name = "karplusStrong.test")
    String karplusStrongTest = "src/test/resources/test.mid";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
Debug.println("volume: " + volume);
    }

    @Test
    @DisplayName("scale")
    void test() throws Exception {
        Synthesizer synthesizer = new KarplusStrongSynthesizer();
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);

        Receiver receiver = synthesizer.getReceiver();
        volume(receiver, volume);

        int[] notes = {60, 62, 64, 65, 67, 69, 71, 72};
        for (int note : notes) {
            ShortMessage on = new ShortMessage(ShortMessage.NOTE_ON, 0, note, 100);
            receiver.send(on, -1);
            Thread.sleep(300);
            ShortMessage off = new ShortMessage(ShortMessage.NOTE_OFF, 0, note, 0);
            receiver.send(off, -1);
        }
        Thread.sleep(1000);

        receiver.close();
        synthesizer.close();
    }

    @Test
    @DisplayName("direct")
    void test2() throws Exception {
        Synthesizer synthesizer = new KarplusStrongSynthesizer();
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);

        Sequencer sequencer = MidiSystem.getSequencer(false);
        Receiver receiver = synthesizer.getReceiver();
        sequencer.getTransmitter().setReceiver(receiver);
        sequencer.open();
Debug.println("sequencer: " + sequencer);

        Path file = Paths.get(karplusStrongTest);

        Sequence seq = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(file)));

        CountDownLatch cdl = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
System.err.println("META: " + meta.getType());
            if (meta.getType() == 47) cdl.countDown();
        };
        volume(receiver, volume);
        sequencer.setSequence(seq);
        sequencer.addMetaEventListener(mel);
System.err.println("START");
        sequencer.start();

if (!onIde) {
 Thread.sleep(time);
 sequencer.stop();
 Debug.println("not on ide");
} else {
        cdl.await();
}
System.err.println("END");
        sequencer.removeMetaEventListener(mel);
        sequencer.close();

        synthesizer.close();
    }

    @Test
    @DisplayName("spi")
    void test3() throws Exception {
Debug.println(karplusStrongTest);
        Synthesizer synthesizer = MidiSystem.getSynthesizer();
Debug.println("synthesizer: " + synthesizer.getClass().getName());
        synthesizer.open();

        Path file = Paths.get(karplusStrongTest);

        Sequence seq = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(file)));

        Sequencer sequencer = MidiSystem.getSequencer(false);
Debug.println("sequencer: " + sequencer.getClass().getName());
        sequencer.open();
        Receiver receiver = synthesizer.getReceiver();
        sequencer.getTransmitter().setReceiver(receiver);

        CountDownLatch cdl = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
System.err.println("META: " + meta.getType());
            if (meta.getType() == 47) cdl.countDown();
        };
        volume(receiver, volume);
        sequencer.setSequence(seq);
        sequencer.addMetaEventListener(mel);
System.err.println("START");
        sequencer.start();

if (!onIde) {
 Thread.sleep(time);
 sequencer.stop();
 Debug.println("not on ide");
} else {
        cdl.await();
}
System.err.println("END");
        sequencer.removeMetaEventListener(mel);

        sequencer.stop();
        sequencer.close();
        synthesizer.close();
    }
}
