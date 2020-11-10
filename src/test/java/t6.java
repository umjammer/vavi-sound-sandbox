/*
 * Copyright (c) 2001 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;

import vavi.util.Debug;


/**
 * MIDI test. (vavi-sound-sandbox)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (vavi)
 * @version 0.00 020703 nsano initial version <br>
 * @see "https://stackoverflow.com/a/45119638/6102938"
 */
public class t6 {

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

    /** plain */
    void tP(String[] args) throws Exception {
        Sequence sequence = MidiSystem.getSequence(new File(args[0]));

        CountDownLatch countDownLatch = new CountDownLatch(1);
        MetaEventListener mel = new MetaEventListener() {
            public void meta(MetaMessage meta) {
System.err.println("META: " + meta.getType());
                if (meta.getType() == 47) {
                    countDownLatch.countDown();
                }
            }
        };
        Sequencer sequencer = MidiSystem.getSequencer(true);
Debug.println("sequencer: " + sequencer);
        sequencer.open();
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(mel);
System.err.println("START: " + args[0]);
        sequencer.start();
        countDownLatch.await();
System.err.println("END: " + args[0]);
        sequencer.removeMetaEventListener(mel);
        sequencer.close();
    }

    /** info */
    void t0(String[] args) throws Exception {
        /** MIDI */
        Synthesizer synthesizer;
        Sequencer sequencer;
        MidiChannel channels[]; 

        // Obtain information about all the installed synthesizers.
        List<MidiDevice.Info> synthInfos = new ArrayList<>();
        MidiDevice device = null;
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        for (int i = 0; i < infos.length; i++) {
            device = MidiSystem.getMidiDevice(infos[i]);
System.err.println("---- [" + i + "] " + infos[i] +" (" + device.getClass().getName() + ")" + " ----");
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
    void t1(String[] args) throws Exception {
        Sequence sequence = MidiSystem.getSequence(new File(args[0]));

        Synthesizer synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);
        // sf
        Soundbank soundbank = synthesizer.getDefaultSoundbank();
        //Instrument[] instruments = synthesizer.getAvailableInstruments();
System.err.println("B: ---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);
        synthesizer.unloadAllInstruments(soundbank);
        String sf2name = "FluidR3 GM2-2.SF2";
//        String sf2name = "Aspirin-Stereo.sf2";
//        String sf2name = "SGM-V2.01.sf2";
        File sf2 = new File("/Users/nsano/lib/audio/sf2", sf2name);
        soundbank = MidiSystem.getSoundbank(sf2);
        synthesizer.loadAllInstruments(soundbank);
System.err.println("A: ---- " + soundbank.getDescription() + " ----");
        //instruments = synthesizer.getAvailableInstruments();
//Arrays.asList(instruments).forEach(System.err::println);
        // volume (not work ???)
//        MidiChannel[] channels = synthesizer.getChannels();
//        double gain = 0.02d;
//        for (int i = 0; i < channels.length; i++) {
//            channels[i].controlChange(7, (int) (gain * 127.0));
//        }

        CountDownLatch countDownLatch = new CountDownLatch(1);
        MetaEventListener mel = new MetaEventListener() {
            public void meta(MetaMessage meta) {
System.err.println("META: " + meta.getType());
                if (meta.getType() == 47) {
                    countDownLatch.countDown();
                }
            }
        };
        Sequencer sequencer = MidiSystem.getSequencer(false);
Debug.println("sequencer: " + sequencer.getClass().getName() + ", " + sequencer.hashCode());
        sequencer.open();
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(mel);
System.err.println("START: " + args[0]);
        sequencer.start();
        countDownLatch.await();
System.err.println("END: " + args[0]);
        sequencer.removeMetaEventListener(mel);
        sequencer.close();

        synthesizer.close();
    }

    /** sf2 direct: work */
    @SuppressWarnings("restriction")
    void t2(String[] args) throws Exception {
        com.sun.media.sound.SoftSynthesizer synthesizer = new com.sun.media.sound.SoftSynthesizer();
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);

        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        sequencer.open();

        String sf2name = "FluidR3 GM2-2.SF2";
//        String sf2name = "Aspirin-Stereo.sf2";
        File sf2 = new File("/Users/nsano/lib/audio/sf2", sf2name);
        com.sun.media.sound.SF2Soundbank bank = new com.sun.media.sound.SF2Soundbank(sf2);
        synthesizer.loadAllInstruments(bank);

        Sequence seq = MidiSystem.getSequence(new File(args[0]));

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
System.err.println("START: " + args[0]);
        sequencer.start();
        countDownLatch.await();
System.err.println("END: " + args[0]);
        sequencer.removeMetaEventListener(mel);
        sequencer.close();

        sequencer.close();
        synthesizer.close();
    }

    void t3(String[] args) throws Exception {
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

//        String filename = "../../../src/sano-n/vavi-games-puyo/src/main/resources/sound/puyo_08.mid";
//        String filename = "m0057_01.mid";
//        String filename = "ac4br_gm.MID";
        String filename = "31137_TSquare.mid";
//        String filename = "thexder.mid";
//        String filename = "THEXDER.mid";
//        String filename = "Firecracker.mid";
//        String filename = "Rydeen.mid";
        File file = new File(System.getProperty("user.home"), "/Music/midi/1/" + filename);
        Sequence seq = MidiSystem.getSequence(file);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        MetaEventListener mel = new MetaEventListener() {
            public void meta(MetaMessage meta) {
//System.err.println("META: " + meta.getType());
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

    /** midi network session test, [1] seemed "session1" */
    void t4(String[] args) throws Exception {
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

System.err.println("END");
        synthesizer.close();
    }

    /**
     * The program entry.
     */
    public static void main(String[] args) throws Exception {
        t6 app = new t6();
//        app.t0(args);
        app.t3(args);
//        app.t4(args);
    }
}

/* */
