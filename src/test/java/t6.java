/*
 * Copyright (c) 2001 by Naohide Sano, All rights rserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;


/**
 * MIDI test.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (vavi)
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
        sequencer.setSequence(sequence);
        sequencer.start();
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
