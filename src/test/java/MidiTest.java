/*
 * Copyright (c) 2001 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;

import org.junit.jupiter.api.BeforeEach;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * MIDI test. (vavi-sound-sandbox)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (vavi)
 * @version 0.00 020703 nsano initial version <br>
 * @see "https://stackoverflow.com/a/45119638/6102938"
 */
@PropsEntity(url = "file:local.properties")
public class MidiTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    static {
//        System.setProperty("javax.sound.midi.Sequencer", "#Real Time Sequencer");
//        System.setProperty("javax.sound.midi.Synthesizer", "#Gervill");
//        System.setProperty("vavi.sound.midi.rococoa.RococoaSynthesizer.audesc", "NiSc:nK1v");
//        System.setProperty("vavi.sound.midi.rococoa.RococoaSynthesizer.audesc", "Ftcr:mc5p");
//        System.setProperty("vavi.sound.midi.rococoa.RococoaSynthesizer.audesc", "DGSB:Dexd");
        System.setProperty("javax.sound.midi.Synthesizer", "#Rococoa MIDI Synthesizer");
//        System.setProperty("javax.sound.midi.Synthesizer", "#JSyn MIDI Synthesizer");
//        System.setProperty("javax.sound.midi.Receiver", "#Unknown name");
//        System.setProperty("javax.sound.midi.Receiver", "#Rococoa MIDI Synthesizer");
    }

    static final float volume = Float.parseFloat(System.getProperty("vavi.test.volume",  "0.2"));

    @Property(name = "sf2")
    String filename;

    @Property(name = "sf2")
    String sf2name = System.getProperty("user.home") + "/Library/Audio/Sounds/Banks/Orchestra/default.sf2";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    /** plain */
    void tP() throws Exception {
        Sequence sequence = MidiSystem.getSequence(new File(filename));

        CountDownLatch countDownLatch = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
Debug.println("META: " + meta.getType());
            if (meta.getType() == 47) {
                countDownLatch.countDown();
            }
        };
        Sequencer sequencer = MidiSystem.getSequencer(true);
Debug.println("sequencer: " + sequencer);
        sequencer.open();
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(mel);
Debug.println("START: " + filename);
        sequencer.start();
        countDownLatch.await();
Debug.println("END: " + filename);
        sequencer.removeMetaEventListener(mel);
        sequencer.close();
    }

    /** info */
    void t0() throws Exception {
        // MIDI
        Synthesizer synthesizer;
        Sequencer sequencer;
        MidiChannel[] channels;

        // Obtain information about all the installed synthesizers.
        List<MidiDevice.Info> synthInfos = new ArrayList<>();
        MidiDevice device = null;
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        for (int i = 0; i < infos.length; i++) {
            device = MidiSystem.getMidiDevice(infos[i]);
Debug.println("---- [" + i + "] " + infos[i] +" (" + device.getClass().getName() + ")" + " ----");
Debug.println("name      : " + infos[i].getName());
Debug.println("vendor    : " + infos[i].getVendor());
Debug.println("descriptor: " + infos[i].getDescription());
Debug.println("version   : " + infos[i].getVersion());
synthInfos.add(infos[i]);
        }

        // Now, display strings from synthInfos list in GUI.

Debug.println("----");
        sequencer = MidiSystem.getSequencer();
Debug.println("default sequencer: " + sequencer.getDeviceInfo());
Debug.println("default sequencer: " + sequencer);
        sequencer.open();

Debug.println("---- t0");
        synthesizer = MidiSystem.getSynthesizer();
Debug.println("default synthesizer: " + synthesizer.getDeviceInfo());
Debug.println("default synthesizer: " + synthesizer);
        channels = synthesizer.getChannels();
Debug.println("channels: " + channels.length);
Debug.println("sound bank: " + synthesizer.getDefaultSoundbank());
Debug.println("instruments: "+ synthesizer.getLoadedInstruments().length);

        Receiver receiver = MidiSystem.getReceiver();
Debug.println("default receiver: " + receiver);

        Transmitter transmitter = MidiSystem.getTransmitter();
Debug.println("default transmitter: " + transmitter);

        sequencer.close();
    }

    /** sf2 by spi: work */
    void t1() throws Exception {
        Sequence sequence = MidiSystem.getSequence(new File(filename));

        Synthesizer synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);
        // sf
        Soundbank soundbank = synthesizer.getDefaultSoundbank();
        //Instrument[] instruments = synthesizer.getAvailableInstruments();
Debug.println("B: ---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);
        synthesizer.unloadAllInstruments(soundbank);
        File sf2 = new File(sf2name);
        if (sf2.exists()) {
            soundbank = MidiSystem.getSoundbank(sf2);
            synthesizer.loadAllInstruments(soundbank);
Debug.println("A: ---- " + soundbank.getDescription() + " ----");
//instruments = synthesizer.getAvailableInstruments();
//Arrays.asList(instruments).forEach(System.err::println);
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
Debug.println("META: " + meta.getType());
            if (meta.getType() == 47) {
                countDownLatch.countDown();
            }
        };
        Sequencer sequencer = MidiSystem.getSequencer(false);
Debug.println("sequencer: " + sequencer.getClass().getName() + ", " + sequencer.hashCode());
        sequencer.open();
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(mel);
Debug.println("START: " + filename);
        sequencer.start();
        countDownLatch.await();
Debug.println("END: " + filename);
        sequencer.removeMetaEventListener(mel);
        sequencer.close();

        synthesizer.close();
    }

    /** sf2 direct: work */
    @SuppressWarnings("restriction")
    void t2() throws Exception {
        com.sun.media.sound.SoftSynthesizer synthesizer = new com.sun.media.sound.SoftSynthesizer();
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);

        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        sequencer.open();

        File sf2 = new File(sf2name);
        com.sun.media.sound.SF2Soundbank bank = new com.sun.media.sound.SF2Soundbank(sf2);
        synthesizer.loadAllInstruments(bank);

        Sequence seq = MidiSystem.getSequence(new File(filename));

        CountDownLatch countDownLatch = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
Debug.println("META: " + meta.getType());
            if (meta.getType() == 47) {
                countDownLatch.countDown();
            }
        };
        sequencer.setSequence(seq);
        sequencer.addMetaEventListener(mel);
Debug.println("START: " + filename);
        sequencer.start();
        countDownLatch.await();
Debug.println("END: " + filename);
        sequencer.removeMetaEventListener(mel);
        sequencer.close();

        sequencer.close();
        synthesizer.close();
    }

    void t3() throws Exception {
        Synthesizer synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);

        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.open();
        Receiver receiver = synthesizer.getReceiver();
Debug.println("receiver: " + receiver);
        sequencer.getTransmitter().setReceiver(receiver);
        sequencer.open();
Debug.println("sequencer: " + sequencer);

        File file = new File(filename);
        Sequence seq = MidiSystem.getSequence(file);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
//Debug.println("META: " + meta.getType());
            if (meta.getType() == 47) {
                countDownLatch.countDown();
            }
        };
        sequencer.setSequence(seq);
        sequencer.addMetaEventListener(mel);
Debug.println("START");
        sequencer.start();
        countDownLatch.await();
Debug.println("END");
        sequencer.removeMetaEventListener(mel);
        sequencer.close();

        sequencer.close();
        synthesizer.close();
    }

    /** midi network session test, [1] seemed "session1" */
    void t4() throws Exception {
        Synthesizer synthesizer = MidiSystem.getSynthesizer();
Debug.println("synthesizer: " + synthesizer.getClass().getName());
        synthesizer.open();

        Receiver receiver = synthesizer.getReceiver();
Debug.println("receiver: " + receiver.getClass().getName());
        MidiDevice.Info info = MidiSystem.getMidiDeviceInfo()[1];
Debug.println("device.info: " + info.getDescription());
        MidiDevice device = MidiSystem.getMidiDevice(info);
Debug.println("device: " + device.getClass().getName());
        device.open();
        Transmitter transmitter = device.getTransmitter();
Debug.println("transmitter: " + transmitter.getClass().getName());
        transmitter.setReceiver(receiver);

        System.in.read();

Debug.println("END");
        synthesizer.close();
    }

    /**
     * The program entry.
     */
    public static void main(String[] args) throws Exception {
        MidiTest app = new MidiTest();
        PropsEntity.Util.bind(app);
//        app.t0();
        app.t3();
//        app.t4();
    }
}

/* */
