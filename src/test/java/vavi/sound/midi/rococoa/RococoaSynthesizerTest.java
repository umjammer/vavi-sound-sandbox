/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.rococoa;

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
import javax.sound.midi.Synthesizer;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.util.Debug;

import static vavi.sound.midi.MidiUtil.volume;


/**
 * RococoaMidiDeviceProviderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/03 umjammer initial version <br>
 */
@EnabledOnOs(OS.MAC)
class RococoaSynthesizerTest {

    //
    // how to make voltage work
    // * make voltage use the same jvm
    // ```
    // % pushd /Library/Application Support/Voltage/VRE13
    // % mv 64 64.off
    // % ln -s $JAVA_13_HOME 64
    // ```
    // * add voltage class path
    // `/Library/Application Support/Voltage/voltage.jar`
    // * run by the same jvm
    //
    static {
//        System.setProperty("vavi.sound.midi.rococoa.RococoaSynthesizer.audesc", "Chry:Vltg"); // normally not work (jvm collision)
//        System.setProperty("vavi.sound.midi.rococoa.RococoaSynthesizer.audesc", "");
//        System.setProperty("vavi.sound.midi.rococoa.RococoaSynthesizer.audesc", "DGSB:Dexd");
//        System.setProperty("vavi.sound.midi.rococoa.RococoaSynthesizer.audesc", "AKai:MpcB");
//        System.setProperty("vavi.sound.midi.rococoa.RococoaSynthesizer.audesc", "NiSc:nK1v");
//        System.setProperty("vavi.sound.midi.rococoa.RococoaSynthesizer.audesc", "Ftcr:mc5p"); // Kairatune
//        System.setProperty("vavi.sound.midi.rococoa.RococoaSynthesizer.audesc", "VmbA:Srge");
        System.setProperty("javax.sound.midi.Sequencer", "#Real Time Sequencer");
        System.setProperty("javax.sound.midi.Synthesizer", "#Rococoa MIDI Synthesizer");
    }

    @Test
    @DisplayName("directly")
    void test() throws Exception {
        Synthesizer synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);

        Sequencer sequencer = MidiSystem.getSequencer(false);
        Receiver receiver = synthesizer.getReceiver();
        sequencer.getTransmitter().setReceiver(receiver);
        sequencer.open();
Debug.println("sequencer: " + sequencer);

//        String filename = "Fusion/YMO - Firecracker.mid";
//        String filename = "Games/Super Mario Bros 2 - Overworld.mid";
        String filename = "test.mid";

//        Path file = Paths.get(System.getProperty("gdrive.home"), "/Music/midi/", filename);
        Path file = Paths.get("src/test/resources/", filename);

        Sequence seq = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(file)));

        CountDownLatch countDownLatch = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
Debug.println("META: " + MetaEvent.valueOf(meta.getType()));
            if (meta.getType() == 47) {
                countDownLatch.countDown();
            }
        };
        sequencer.setSequence(seq);
        sequencer.addMetaEventListener(mel);
Debug.println("START");
        sequencer.start();

        volume(receiver, .2f); // volume works?

if (Boolean.valueOf(System.getProperty("vavi.test"))) {
 Thread.sleep(10 * 1000);
 sequencer.stop();
 Debug.println("STOP");
} else {
        countDownLatch.await();
}
Debug.println("END");
        sequencer.removeMetaEventListener(mel);
        sequencer.close();

        sequencer.close();
        synthesizer.close();
    }

    @Test
    @Disabled
    @DisplayName("by spi")
    void test1() throws Exception {
        Synthesizer synthesizer = new RococoaSynthesizer();
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);

        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        sequencer.open();
Debug.println("sequencer: " + sequencer);

//        String filename = "Games/Super Mario Bros 2 - overworld.mid";
        String filename = "test.mid";

//        Path file = Paths.get(System.getProperty("gdrive.home"), "/Music/midi/", filename);
        Path file = Paths.get("src/test/resources/", filename);

        Sequence seq = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(file)));

        CountDownLatch countDownLatch = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
Debug.println("META: " + MetaEvent.valueOf(meta.getType()));
            if (meta.getType() == 47) {
                countDownLatch.countDown();
            }
        };
        sequencer.setSequence(seq);
        sequencer.addMetaEventListener(mel);
Debug.println("START");
        sequencer.start();
if (Boolean.valueOf(System.getProperty("vavi.test"))) {
 Thread.sleep(10 * 1000);
 sequencer.stop();
 Debug.println("STOP");
} else {
 countDownLatch.await();
}
Debug.println("END");
        sequencer.removeMetaEventListener(mel);
        sequencer.close();

        sequencer.close();
        synthesizer.close();
    }
}

/* */
