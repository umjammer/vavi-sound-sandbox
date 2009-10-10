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
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 080526 nsano initial version <br>
 */
public class Test {

    public static void main(String[] args) throws Exception {
//        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
//        for (MidiDevice.Info info : infos) {
//            Debug.println(new String(info.getName().getBytes("ISO8859-1")));
//        }

        String infile = args[0];
        final String outfile = args[1];

        final Sequencer sequencer = MidiSystem.getSequencer();
        sequencer.open();
        Sequence sequence = MidiSystem.getSequence(new File(infile));
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(new MetaEventListener() {
            StringBuilder sb = new StringBuilder();
            public void meta(MetaMessage meta) {
//Debug.println(meta.getType());
                switch (meta.getType()) {
                case 1:  // �e�L�X�g�E�C�x���g 127 bytes
//Debug.println(new String(meta.getData()));
                    String text = new String(meta.getData());
                    String parts[] = text.split(":");
Debug.println(parts[0] + ":" + parts[1] + ":" + meta.getData().length);
                    sb.append(parts[2]);
                    break;
                case 3:  // �g���b�N�� "Master Track", "Voice1"
                    String trackName = new String(meta.getData());
Debug.println(trackName);
                    break;
                case 81: // �e���|�ݒ�
                    break;
                case 88: // ���q�L��
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
                case 6: // �f�[�^�G���g�� MSB
Debug.println("�f�[�^�G���g�� MSB: " + event.getData2());
                    break;
                case 38: // �f�[�^�G���g�� LSB
Debug.println("�f�[�^�G���g�� LSB: " + event.getData2());
                    break;
                default:
Debug.println("unhandled control change: " + event.getData1());
                    break;
                }
                
            }
        }, null);
        sequencer.start();
    }
}

/* */
