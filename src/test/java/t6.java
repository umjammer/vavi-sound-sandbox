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


/**
 * MIDI test.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (vavi)
 * @version 0.00 020703 nsano initial version <br>
 */
public class t6 {

    /** MIDI */
    Synthesizer synthesizer;
    Sequencer sequencer;
    MidiChannel channels[]; 

    /** */
    t6(String[] args) throws Exception {
        // Obtain information about all the installed synthesizers.
        List<MidiDevice.Info> synthInfos = new ArrayList<>();
        MidiDevice device = null;
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        for (int i = 0; i < infos.length; i++) {
            device = MidiSystem.getMidiDevice(infos[i]);
            if (device instanceof Synthesizer) {
System.err.println("---- " + infos[i] + " ----");
System.err.println("name      : " + infos[i].getName());
System.err.println("vendor    : " + infos[i].getVendor());
System.err.println("descriptor: " + infos[i].getDescription());
System.err.println("version   : " + infos[i].getVersion());
synthInfos.add(infos[i]);
            }
        }

        // Now, display strings from synthInfos list in GUI.    

//      if (!(device.isOpen())) {
//          device.open();
//      }

System.err.println("----");
        sequencer = MidiSystem.getSequencer();
System.err.println("default sequencer: " + sequencer.getDeviceInfo());
System.err.println("default sequencer: " + sequencer);
        sequencer.open();

System.err.println("----");
        synthesizer = MidiSystem.getSynthesizer();
System.err.println("default synthesizer: " + synthesizer.getDeviceInfo());
System.err.println("default synthesizer: " + synthesizer);
        channels = synthesizer.getChannels();
System.err.println("channels: " + channels.length);
System.err.println("sound bank: " + synthesizer.getDefaultSoundbank());
System.err.println("instruments: "+ synthesizer.getLoadedInstruments().length);

        Receiver receiver = MidiSystem.getReceiver();
System.err.println("receiver: " + receiver);

        Sequence sequence = MidiSystem.getSequence(new File(args[0]));

        Synthesizer synthesizer = MidiSystem.getSynthesizer();
        // sf
        Soundbank soundbank = synthesizer.getDefaultSoundbank();
        //Instrument[] instruments = synthesizer.getAvailableInstruments();
System.err.println("---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);
//        synthesizer.unloadAllInstruments(soundbank);
//        File file = new File("/Users/nsano/lib/audio/sf2/SGM-V2.01.sf2");
//        soundbank = MidiSystem.getSoundbank(file);
//        synthesizer.loadAllInstruments(soundbank);
        //instruments = synthesizer.getAvailableInstruments();
//System.err.println("---- " + soundbank.getDescription() + " ----");
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
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(mel);
System.err.println("START: " + args[0]);
        sequencer.start();
        countDownLatch.await();
System.err.println("END: " + args[0]);
        sequencer.removeMetaEventListener(mel);
        sequencer.close();
    }

    /**
     * The program entry.
     */
    public static void main(String[] args) throws Exception {
        new t6(args);
        System.exit(0);
    }
}

/* */
