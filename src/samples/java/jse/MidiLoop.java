package jse;
/*
 *        MidiLoop.java
 *
 *        This file is part of the Java Sound Examples.
 */
/*
 *  Copyright (c) 2002 by Florian Bomers <florian@jsresources.org>
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
import java.io.IOException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;


/*        +DocBookXML
        <title>Receiving and sending MIDI (MIDI thru)</title>

        <formalpara><title>Purpose</title>
        <para>Outputs all MIDI events that arrive at a MIDI IN port
        to a MIDI OUT port.</para>
        </formalpara>

        <formalpara><title>Level</title>
        <para>advanced</para></formalpara>

        <formalpara><title>Usage</title>
        <para>
        <cmdsynopsis><command>java MidiLoop</command>
        <arg choice="plain"><replaceable class="parameter">-l</replaceable></arg>
        <arg choice="plain"><replaceable class="parameter">input device</replaceable></arg>
        <arg choice="plain"><replaceable class="parameter">output device</replaceable></arg>
        </cmdsynopsis>
        </para></formalpara>

        <formalpara><title>Parameters</title>
        <variablelist>
        <varlistentry>
        <term><replaceable class="parameter">-l</replaceable></term>
        <listitem><para>List available MIDI devices</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><replaceable class="parameter">input device</replaceable></term>
        <listitem><para>MIDI IN device name</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><replaceable class="parameter">output device</replaceable></term>
        <listitem><para>MIDI OUT device</para></listitem>
        </varlistentry>
        </variablelist>
        </formalpara>

        <formalpara><title>Bugs, limitations</title>
        <para>None.</para>
        </formalpara>

        <formalpara><title>Source code</title>
        <para>
        <ulink url="MidiLoop.java.html">MidiLoop.java</ulink>
        </para>
        </formalpara>

-DocBookXML
*/
public class MidiLoop {
    public static void main(String[] args) {
        try {
            if (args.length > 0) {
                if (args[0].equals("-l")) {
                    listDevicesAndExit(true, true);
                }
            }

            if (args.length != 2) {
                printUsageAndExit();
            }

            String inDeviceName = args[0];
            String outDeviceName = args[1];

            out("getting input device '" + inDeviceName + "'...");

            MidiDevice.Info info = getMidiDeviceInfo(inDeviceName, false);
            if (info == null) {
                err("no input device found for name " + inDeviceName);
            }

            MidiDevice inputDevice = MidiSystem.getMidiDevice(info);
            out("opening input device '" + inDeviceName + "'...");
            inputDevice.open();
            try {
                out("getting output device '" + outDeviceName + "'...");
                info = getMidiDeviceInfo(outDeviceName, true);
                if (info == null) {
                    err("no output device found for name " + outDeviceName);
                }

                MidiDevice outputDevice = MidiSystem.getMidiDevice(info);
                out("opening output device '" + outDeviceName + "'...");
                outputDevice.open();
                try {
                    out("connecting input with output...");
                    inputDevice.getTransmitter().setReceiver(outputDevice.getReceiver());

                    out("connected. Press ENTER to quit.");
                    System.in.read();
                } finally {
                    out("Closing output device...");
                    outputDevice.close();
                }
            } finally {
                out("Closing input device...");
                inputDevice.close();
            }
        } catch (IOException ioe) {
            out(ioe);
        } catch (MidiUnavailableException mue) {
            out(mue);
        }
        System.exit(0);
    }

    private static void printUsageAndExit() {
        out("MidiLoop usage:");
        out("  java MidiLoop [-l] <input device name> <output device name>");
        out("    -l\tlist available devices and exit");
        out("    <input device name>\tinput to named device");
        out("    <output device name>\toutput to named device");
        System.exit(0);
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

        //out("Searching '"+strDeviceName+"' for "+(forOutput?"output":"input"));
        for (int i = 0; i < aInfos.length; i++) {
            if (aInfos[i].getName().equals(strDeviceName)) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(aInfos[i]);
                    boolean bAllowsInput = (device.getMaxTransmitters() != 0);
                    boolean bAllowsOutput = (device.getMaxReceivers() != 0);

                    //out("Looking for at '"+aInfos[i].getName()+"' with "+dev.getMaxReceivers()+" receivers and "+dev.getMaxTransmitters()+" transmitters.");
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

    private static void err(String strMessage) {
        out(strMessage);
        System.exit(1);
    }

    private static void out(String strMessage) {
        System.out.println(strMessage);
    }

    private static void out(Throwable t) {
        t.printStackTrace();
    }
}

/* */
