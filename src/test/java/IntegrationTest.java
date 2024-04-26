/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;


/**
 * IntegrationTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-03-02 nsano initial version <br>
 */
public class IntegrationTest {

    /**
     * @param args none
     */
    public static void main(String[] args) throws Exception {
        MidiDevice device = null;
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        for (int i = 0; i < infos.length; i++) {
            device = MidiSystem.getMidiDevice(infos[i]);
System.err.println("---- [" + i + "] " + infos[i] +" (" + device.getClass().getName() + ")" + " ----");
System.err.println("name      : " + infos[i].getName());
System.err.println("vendor    : " + infos[i].getVendor());
System.err.println("descriptor: " + infos[i].getDescription());
System.err.println("version   : " + infos[i].getVersion());
        }
    }

    // for javaassist test
    java.util.List  $_;
    void x() { java.util.List r = new java.util.ArrayList(); for (int i = 0; i < $_.size(); i++) { if (!$_.get(i).getClass().getName().contains("SoftMidiAudio")) { r.add($_.get(i)); }} $_ = r; }
}
