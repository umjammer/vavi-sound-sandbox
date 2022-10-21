/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.vsq;

import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * LineTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080526 nsano initial version <br>
 */
public class Test2 {

    static {
        System.setProperty("javax.sound.midi.Sequencer", "#Real Time Sequencer");
        System.setProperty("javax.sound.midi.Synthesizer", "#Gervill");
    }

    public static void main(String[] args) throws Exception {
        String infile = args[0];

        CountDownLatch cdl = new CountDownLatch(1);

        Sequence sequence = MidiSystem.getSequence(new File(infile));
        VSQ vsq = new VSQ(sequence);
        vsq.convert1(sequence);
        Sequencer sequencer = MidiSystem.getSequencer();
        sequencer.open();
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(new MetaEventListener() {
            public void meta(javax.sound.midi.MetaMessage message) {
                switch (MetaEvent.valueOf(message.getType())) {
                case META_MACHINE_DEPEND: // シーケンサ固有のメタイベント
                    byte[] data = message.getData();
Debug.printf("%02X, %s\n", data[0], StringUtil.getDump(data));
                    break;
                case META_TEXT_EVENT:  // テキスト・イベント 127 bytes
//Debug.println(new String(meta.getData()));
                    String text = new String(message.getData(), Charset.forName("MS932"));
                    if (!text.startsWith("DM")) {
                        String[] parts = text.split(":");
Debug.println(parts[0] + ":" + parts[1] + ":" + message.getData().length/* + "\n" + parts[2]*/);
                    }
                    break;
                case META_END_OF_TRACK:
                    cdl.countDown();
                    break;
                default:
                    break;
                }
            }
        });
        sequencer.start();
        cdl.await();
        sequencer.stop();
        sequencer.close();
    }
}

/* */
