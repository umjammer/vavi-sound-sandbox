package jse;
/*
 *        MidiNote.java
 *
 *        This file is part of the Java Sound Examples.
 */
/*
 *  Copyright (c) 1999 - 2001 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;


// TODO: an optional delay parameter that is added to getMicrosecondPosition to be used as timestamp for the event delivery.

/*        +DocBookXML
        <title>Playing a note on a MIDI device</title>

        <formalpara><title>Purpose</title>
        <para>Plays a single note on a MIDI device. The MIDI device can
        be a software synthesizer, an internal hardware synthesizer or
        any device connected to the MIDI OUT port.</para>
        </formalpara>

        <formalpara><title>Level</title>
        <para>advanced</para></formalpara>

        <formalpara><title>Usage</title>
        <para>
        <cmdsynopsis><command>java MidiNote</command>
        <arg choice="plain"><replaceable class="parameter">keynumber</replaceable></arg>
        <arg choice="plain"><replaceable class="parameter">velocity</replaceable></arg>
        <arg choice="plain"><replaceable class="parameter">duration</replaceable></arg>
        </cmdsynopsis>
        </para></formalpara>

        <formalpara><title>Parameters</title>
        <variablelist>
        <varlistentry>
        <term><replaceable class="parameter">keynumber</replaceable></term>
        <listitem><para>the MIDI key number</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><replaceable class="parameter">velocity</replaceable></term>
        <listitem><para>the velocity</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><replaceable class="parameter">duration</replaceable></term>
        <listitem><para>the duration in milliseconds</para></listitem>
        </varlistentry>
        </variablelist>
        </formalpara>

        <formalpara><title>Bugs, limitations</title>
        <para>Not well-tested.</para>
        </formalpara>

        <formalpara><title>Source code</title>
        <para>
        <ulink url="MidiNote.java.html">MidiNote.java</ulink>
        </para>
        </formalpara>

-DocBookXML
*/
public class MidiNote {
    /**        Flag for debugging messages.
             If true, some messages are dumped to the console
             during operation.
    */
    private static boolean DEBUG = false;

    public static void main(String[] args) {
        try {
            // TODO: make settable via command line
            int nChannel = 0;

            int nKey = 0; // MIDI key number
            int nVelocity = 0;

            /*
             *        Time between note on and note off event in
             *        milliseconds. Note that on most systems, the
             *        best resolution you can expect are 10 ms.
             */
            int nDuration = 0;
            int nArgumentIndexOffset = 0;
            String strDeviceName = null;
            if (args.length == 4) {
                strDeviceName = args[0];
                nArgumentIndexOffset = 1;
            } else if (args.length == 3) {
                nArgumentIndexOffset = 0;
            } else {
                printUsageAndExit();
            }
            nKey = Integer.parseInt(args[0 + nArgumentIndexOffset]);
            nKey = Math.min(127, Math.max(0, nKey));
            nVelocity = Integer.parseInt(args[1 + nArgumentIndexOffset]);
            nVelocity = Math.min(127, Math.max(0, nVelocity));
            nDuration = Integer.parseInt(args[2 + nArgumentIndexOffset]);
            nDuration = Math.max(0, nDuration);

            MidiDevice outputDevice = null;
            Receiver receiver = null;
            if (strDeviceName != null) {
                MidiDevice.Info info = getMidiDeviceInfo(strDeviceName, true);
                if (info == null) {
                    out("no device info found for name " + strDeviceName);
                    System.exit(1);
                }
                try {
                    outputDevice = MidiSystem.getMidiDevice(info);
                    outputDevice.open();
                } catch (MidiUnavailableException e) {
                    if (DEBUG) {
                        out(e);
                    }
                }
                if (outputDevice == null) {
                    out("wasn't able to retrieve MidiDevice");
                    System.exit(1);
                }
                try {
                    receiver = outputDevice.getReceiver();
                } catch (MidiUnavailableException e) {
                    if (DEBUG) {
                        out(e);
                    }
                }
            } else {
                /*        We retrieve a Receiver for the default
                        MidiDevice.
                */
                try {
                    receiver = MidiSystem.getReceiver();
                } catch (MidiUnavailableException e) {
                    if (DEBUG) {
                        out(e);
                    }
                }
            }
            if (receiver == null) {
                out("wasn't able to retrieve Receiver");
                System.exit(1);
            }

            /*        Here, we prepare the MIDI messages to send.
                    Obviously, one is for turning the key on and
                    one for turning it off.
            */
            MidiMessage onMessage = null;
            MidiMessage offMessage = null;
            try {
                onMessage = new ShortMessage();
                offMessage = new ShortMessage();
                ((ShortMessage) onMessage).setMessage(ShortMessage.NOTE_ON,
                                                      nChannel, nKey, nVelocity);
                ((ShortMessage) offMessage).setMessage(ShortMessage.NOTE_OFF,
                                                       nChannel, nKey);

                /* test for SysEx messages */

                //byte[] data = { (byte) 0xF0, (byte) 0xF7, (byte) 0x99, 0x40, 0x7F, 0x40, 0x00 };
                //onMessage = new SysexMessage();
                //offMessage = new SysexMessage();
                //onMessage.setMessage(data, data.length);
                //offMessage = (SysexMessage) onMessage.clone();
            } catch (InvalidMidiDataException e) {
                if (DEBUG) {
                    out(e);
                }
            }

            /*
             *        Turn the note on
             */
            receiver.send(onMessage, -1);

            /*
             *        Wait for the specified amount of time
             *        (the duration of the note).
             */
            try {
                Thread.sleep(nDuration);
            } catch (InterruptedException e) {
                if (DEBUG) {
                    out(e);
                }
            }

            /*
             *        Turn the note off.
             */
            receiver.send(offMessage, -1);

            /*
             *        Clean up.
             */
            receiver.close();
            if (outputDevice != null) {
                outputDevice.close();
            }
        } catch (Throwable t) {
            out(t);
        }
        System.exit(0);
    }

    private static void printUsageAndExit() {
        out("MidiNote: usage:");
        out("  java MidiNote [<device name>] <note number> <velocity> <duration>");
        out("    <device name>\toutput to named device");
        out("    -D\tenables debugging output");
        System.exit(1);
    }

    private static void listDevicesAndExit(boolean forInput, boolean forOutput) {
        if (forInput && !forOutput) {
            out("Available MIDI IN Devices:");
        } else if (!forInput && forOutput) {
            out("Available MIDI OUT Devices:");
        } else {
            out("Available MIDI Devices:");
        }

        MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < aInfos.length; i++) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(aInfos[i]);
                boolean bAllowsInput = (device.getMaxTransmitters() != 0);
                boolean bAllowsOutput = (device.getMaxReceivers() != 0);
                if ((bAllowsInput && forInput) || (bAllowsOutput && forOutput)) {
                    out("" + i + "  " + (bAllowsInput ? "IN " : "   ") +
                        (bAllowsOutput ? "OUT " : "    ") +
                        aInfos[i].getName() + ", " + aInfos[i].getVendor() +
                        ", " + aInfos[i].getVersion() + ", " +
                        aInfos[i].getDescription());
                }
            } catch (MidiUnavailableException e) {
                // device is obviously not available...
            }
        }
        if (aInfos.length == 0) {
            out("[No devices available]");
        }
        System.exit(0);
    }

    /*
     *        This method tries to return a MidiDevice.Info whose name
     *        matches the passed name. If no matching MidiDevice.Info is
     *        found, null is returned.
     *        If forOutput is true, then only output devices are searched,
     *        otherwise only input devices.
     */
    private static MidiDevice.Info getMidiDeviceInfo(String strDeviceName,
                                                     boolean forOutput) {
        MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < aInfos.length; i++) {
            if (aInfos[i].getName().equals(strDeviceName)) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(aInfos[i]);
                    boolean bAllowsInput = (device.getMaxTransmitters() != 0);
                    boolean bAllowsOutput = (device.getMaxReceivers() != 0);
                    if ((bAllowsOutput && forOutput) ||
                        (bAllowsInput && !forOutput)) {
                        return aInfos[i];
                    }
                } catch (MidiUnavailableException mue) {
                }
            }
        }
        return null;
    }

    private static void out(String strMessage) {
        System.out.println(strMessage);
    }

    private static void out(Throwable t) {
        t.printStackTrace();
    }
}
