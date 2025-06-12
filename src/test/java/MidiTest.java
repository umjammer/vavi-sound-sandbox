/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import vavi.sound.midi.MidiConstants;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.sound.midi.MidiUtil.volume;


/**
 * MIDI test. (vavi-sound-sandbox)
 * <p>
 * DETERMINE default sequencer and synthesizer at static block at the top of this code.
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

    @Property(name = "sequencer")
    String sequencer = "#Real Time Sequencer";

    @Property(name = "synthesizer")
    String synthesizer = "#Gervill";

    @Property(name = "receiver")
    String receiver;

    @Property(name = "rococoa.audesc")
    String audesc;

    @Property(name = "vavi.test.volume.midi")
    float volume = 0.2f;

    @Property(name = "midi.test")
    String filename;

    @Property(name = "sf2")
    String sf2name = System.getProperty("user.home") + "/Library/Audio/Sounds/Banks/Orchestra/default.sf2";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

Debug.println("volume: " + volume);
        System.setProperty("javax.sound.midi.Sequencer", sequencer);
Debug.println("sequencer: " + System.getProperty("javax.sound.midi.Sequencer"));

        System.setProperty("javax.sound.midi.Synthesizer", synthesizer);
Debug.println("synthesizer: " + System.getProperty("javax.sound.midi.Synthesizer"));
        if (receiver != null) {
            System.setProperty("javax.sound.midi.Receiver", receiver);
Debug.println("receiver: " + System.getProperty("javax.sound.midi.Receiver"));
        }

        if (audesc != null) {
            System.setProperty("vavi.sound.midi.rococoa.RococoaSynthesizer.audesc", audesc);
Debug.println("audesc: " + System.getProperty("vavi.sound.midi.rococoa.RococoaSynthesizer.audesc"));
        }
    }

    /** plain */
    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void tP() throws Exception {
        Sequence sequence = MidiSystem.getSequence(new File(filename));

        CountDownLatch cdl = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
Debug.println("META: " + meta.getType());
            if (meta.getType() == 47) cdl.countDown();
        };
        Sequencer sequencer = MidiSystem.getSequencer(true);
Debug.println("sequencer: " + sequencer);
        sequencer.open();
        volume(sequencer.getReceiver(), volume);
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(mel);
Debug.println("START: " + filename);
        sequencer.start();
        cdl.await();
Debug.println("END: " + filename);
        sequencer.removeMetaEventListener(mel);
        sequencer.close();
    }

    /** */
    static String getInOut(MidiDevice device) {
        if (device.getMaxTransmitters() == 0 && device.getMaxReceivers() == 0)
            return "UNKNOWN(t:" + device.getMaxTransmitters() + ", r:" + device.getMaxReceivers() + ")";
        else if (device.getMaxTransmitters() == 0)
            return "INPUT";
        else if (device.getMaxReceivers() == 0)
            return "OUTPUT";
        else
            return "INOUT(t:" + device.getMaxTransmitters() + ", r:" + device.getMaxReceivers() + ")";
    }

    /**
     * info
     * @see "https://bonar.hatenablog.com/entry/20090322/1237711377"
     */
    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
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
System.err.println("---- [" + i + "] " + infos[i] +" (" + device.getClass().getName() + ")" + " " + getInOut(device) + " ----");
System.err.println("name      : " + infos[i].getName());
System.err.println("vendor    : " + infos[i].getVendor());
System.err.println("descriptor: " + infos[i].getDescription());
System.err.println("version   : " + infos[i].getVersion());
synthInfos.add(infos[i]);
        }

        // Now, display strings from synthInfos list in GUI.

System.err.println("----");
        sequencer = MidiSystem.getSequencer();
System.err.println("default sequencer: " + sequencer.getDeviceInfo());
System.err.println("default sequencer: " + sequencer);
        sequencer.open();

System.err.println("---- t0");
        synthesizer = MidiSystem.getSynthesizer();
System.err.println("default synthesizer: " + synthesizer.getDeviceInfo());
System.err.println("default synthesizer: " + synthesizer);
        channels = synthesizer.getChannels();
System.err.println("channels: " + channels.length);
System.err.println("sound bank: " + synthesizer.getDefaultSoundbank());
System.err.println("instruments: "+ synthesizer.getLoadedInstruments().length);

        Receiver receiver = MidiSystem.getReceiver();
System.err.println("default receiver: " + receiver);

        Transmitter transmitter = MidiSystem.getTransmitter();
System.err.println("default transmitter: " + transmitter);

        sequencer.close();
    }

    /** sf2 by spi: work */
    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void t1() throws Exception {
        Sequence sequence = MidiSystem.getSequence(new File(filename));

        Synthesizer synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);
        // sf
        Soundbank soundbank = synthesizer.getDefaultSoundbank();
//Instrument[] instruments = synthesizer.getAvailableInstruments();
if (soundbank != null) {
 Debug.println("B: ---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);
 synthesizer.unloadAllInstruments(soundbank);
}
        File sf2 = new File(sf2name);
Debug.println("soundfont: " + sf2);
        if (sf2.exists()) {
            soundbank = MidiSystem.getSoundbank(sf2);
            synthesizer.loadAllInstruments(soundbank);
Debug.println("A: ---- " + soundbank.getDescription() + " ----");
//instruments = synthesizer.getAvailableInstruments();
//Arrays.asList(instruments).forEach(System.err::println);
        } else {
Debug.println("WARNING: " + sf2 + " does not found!");
        }

        CountDownLatch cdl = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
Debug.println("META: " + MidiConstants.MetaEvent.valueOf(meta.getType()));
            if (meta.getType() == 47) cdl.countDown();
        };
        Sequencer sequencer = MidiSystem.getSequencer(false);
Debug.println("sequencer: " + sequencer.getClass().getName() + ", " + sequencer.hashCode());
        sequencer.open();
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        volume(synthesizer.getReceiver(), volume);
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(mel);
Debug.println("START: " + filename);
        sequencer.start();
        cdl.await();
Debug.println("END: " + filename);
        sequencer.removeMetaEventListener(mel);
        sequencer.close();

        synthesizer.close();
    }

    /** sf2 direct: work */
    @SuppressWarnings("restriction")
    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
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

        CountDownLatch cdl = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
Debug.println("META: " + meta.getType());
            if (meta.getType() == 47) cdl.countDown();
        };
        volume(sequencer.getReceiver(), volume);
        sequencer.setSequence(seq);
        sequencer.addMetaEventListener(mel);
Debug.println("START: " + filename);
        sequencer.start();
        cdl.await();
Debug.println("END: " + filename);
        sequencer.removeMetaEventListener(mel);
        sequencer.close();

        sequencer.close();
        synthesizer.close();
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void t3() throws Exception {
Debug.println(filename);
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
        volume(receiver, volume);

        File file = new File(filename);
        Sequence seq = MidiSystem.getSequence(file);

        CountDownLatch cdl = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
//Debug.println("META: " + meta.getType());
            if (meta.getType() == 47) cdl.countDown();
        };
        volume(receiver, volume);
        sequencer.setSequence(seq);
        sequencer.addMetaEventListener(mel);
Debug.println("START");
        sequencer.start();
        cdl.await();
Debug.println("END");
        sequencer.removeMetaEventListener(mel);
        sequencer.close();

        sequencer.close();
        synthesizer.close();
    }

    /** midi network session test, [1] seemed "session1" */
    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
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
        app.setup();
        app.tP();
//        app.t0();
//        app.t1();
//        app.t2();
//        app.t3();
//        app.t4();
    }
}
