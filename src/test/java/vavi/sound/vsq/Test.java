/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.vsq;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;

import vavi.util.Debug;


/**
 * Test.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080526 nsano initial version <br>
 */
public class Test {

    static {
        System.setProperty("javax.sound.midi.Sequencer", "#Real Time Sequencer");
        System.setProperty("javax.sound.midi.Synthesizer", "#Gervill");
    }

    public static void main(String[] args) throws Exception {
//        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
//        for (MidiDevice.Info info : infos) {
//            Debug.println(new String(info.getName().getBytes("ISO8859-1")));
//        }

        String infile = args[0];
        final String outfile = args[1];

        CountDownLatch cdl = new CountDownLatch(1);

        final Sequencer sequencer = MidiSystem.getSequencer();
Debug.println(sequencer);
        sequencer.open();
        Sequence sequence = MidiSystem.getSequence(new File(infile));
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(new MetaEventListener() {
            StringBuilder sb = new StringBuilder();
            public void meta(MetaMessage meta) {
//Debug.println(meta.getType());
                switch (meta.getType()) {
                case 1:  // テキスト・イベント 127 bytes
//Debug.println(new String(meta.getData()));
                    String text = new String(meta.getData(), Charset.forName("MS932"));
                    String parts[] = text.split(":");
Debug.println(parts[0] + ":" + parts[1] + ":" + meta.getData().length);
                    sb.append(parts[2]);
                    break;
                case 3:  // トラック名 "Master Track", "Voice1"
                    String trackName = new String(meta.getData());
Debug.println(trackName);
                    break;
                case 81: // テンポ設定
                    break;
                case 88: // 拍子記号
                    break;
                case 47:
try {
 Writer writer = new FileWriter(outfile);
 writer.write(sb.toString());
 writer.flush();
 writer.close();
} catch (IOException e) {
 e.printStackTrace(System.err);
}
                    cdl.countDown();
                    break;
                default:
Debug.println("unhandled meta: " + meta.getType());
                    break;
                }
            }
        });
        sequencer.addControllerEventListener(new ControllerEventListener() {
            public void controlChange(ShortMessage event) {
                switch (event.getData1()) {
                case 98: // NRPN LSB
Debug.println("NRPN LSB: " + event.getData2());
                    break;
                case 99: // NRPN MSB
Debug.println("NRPN MSB: " + event.getData2());
                    break;
                case 6: // データエントリ MSB
Debug.println("データエントリ MSB: " + event.getData2());
                    break;
                case 38: // データエントリ LSB
Debug.println("データエントリ LSB: " + event.getData2());
                    break;
                default:
Debug.println("unhandled control change: " + event.getData1());
                    break;
                }

            }
        }, null);
        sequencer.start();
        cdl.await();
        sequencer.stop();
        sequencer.close();
    }
}

/* */
