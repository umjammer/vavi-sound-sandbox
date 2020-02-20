/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.vsq;

import java.io.File;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

import vavi.sound.midi.MidiConstants;
import vavi.sound.midi.MidiUtil;
import vavi.util.Debug;


/**
 * Test2.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080526 nsano initial version <br>
 */
public class Test2 {

    public static void main(String[] args) throws Exception {
        String infile = args[0];
        Sequence sequence = MidiSystem.getSequence(new File(infile));
        VSQ vsq = new VSQ(sequence);
        vsq.convert1(sequence);
        Sequencer sequencer = MidiSystem.getSequencer();
        sequencer.open();
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(new MetaEventListener() {
            public void meta(javax.sound.midi.MetaMessage message) {
                switch (message.getType()) {
                case MidiConstants.META_MACHINE_DEPEND: // シーケンサ固有のメタイベント
Debug.println(MidiUtil.getDecodedMessage(message.getData()));
                    break;
                default:
                    break;
                }
            }
        });
        sequencer.start();
        while (sequencer.isRunning()) {
            try { Thread.sleep(100); } catch (Exception e) {}
        }
        sequencer.stop();
        sequencer.close();
    }
}

/* */
