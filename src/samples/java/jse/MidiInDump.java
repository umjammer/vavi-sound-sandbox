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

package jse;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

import gnu.getopt.Getopt;

import static java.lang.System.getLogger;


/*        +DocBookXML
        <title>Listens to a MIDI port and dump the received event to the console</title>

        <formalpara><title>Purpose</title>
        <para>Listens to a MIDI port and dump the received event to the console.</para></formalpara>

        <formalpara><title>Level</title>
        <para>advanced</para></formalpara>

        <formalpara><title>Usage</title>
        <para>
        <!--cmdsynopsis>
        <command>java MidiInDump</command>
        <arg choice="plain"><option>-l</option></arg>
        </cmdsynopsis-->
        <cmdsynopsis>
        <command>java MidiInDump</command>
        <arg choice="plain"><option>-d <replaceable>devicename</replaceable></option></arg>
        </cmdsynopsis>
        </para></formalpara>

        <formalpara><title>Parameters</title>
        <variablelist>
        <!--varlistentry>
        <term><option>-l</option></term>
        <listitem><para>list the availabe MIDI devices</para></listitem>
        </varlistentry-->
        <varlistentry>
        <term><option>-d <replaceable>devicename</replaceable></option></term>
        <listitem><para>reads from named device (see <option>-l</option>)</para></listitem>
        </varlistentry>
        </variablelist>
        </formalpara>

        <formalpara><title>Bugs, limitations</title>
        <para>
        For the Sun J2SDK 1.3.x or 1.4.0, MIDI IN does not work. See the <ulink url="http://www.jsresources.org/faq/">Java Sound Programmer's FAQ</ulink> for alternatives.
        </para></formalpara>

        <formalpara><title>Source code</title>
        <para>
        <ulink url="MidiInDump.java.html">MidiInDump.java</ulink>,
        <ulink url="DumpReceiver.java.html">DumpReceiver.java</ulink>,
        <ulink url="http://www.urbanophile.com/arenn/hacking/download.html">gnu.getopt.Getopt</ulink>
        </para>
        </formalpara>

-DocBookXML
*/

/**
 * MidiInDump.java
 * <p>
 * This file is part of the Java Sound Examples.
 */
public class MidiInDump {

    private static final Logger logger = getLogger(MidiInDump.class.getName());

    public static void main(String[] args) {
        try {
            // The device name to listen to.
            String strDeviceName = null;

            // Parsing of command-line options takes place...
            Getopt g = new Getopt("MidiInDump", args, "hld:D");
            int c;
            while ((c = g.getopt()) != -1) {
                switch (c) {
                    case 'h':
                        printUsageAndExit();
                    case 'l':
                        listDevicesAndExit();
                    case 'd':
                        strDeviceName = g.getOptarg();
                        logger.log(Level.DEBUG, "MidiInDump.main(): device name: " + strDeviceName);
                        break;
                    case '?':
                        printUsageAndExit();
                    default:
                        logger.log(Level.DEBUG, "MidiInDump.main(): getopt() returned " + c);
                        break;
                }
            }

            if (strDeviceName == null) {
                logger.log(Level.DEBUG, "device name not specified!");
                printUsageAndExit();
            }

            MidiDevice.Info info = getMidiDeviceInfo(strDeviceName, false);
            if (info == null) {
                logger.log(Level.DEBUG, "no device info found for name " + strDeviceName);
                System.exit(1);
            }

            MidiDevice inputDevice = null;
            try {
                inputDevice = MidiSystem.getMidiDevice(info);
                inputDevice.open();
            } catch (MidiUnavailableException e) {
                logger.log(Level.DEBUG, e);
            }
            if (inputDevice == null) {
                logger.log(Level.DEBUG, "wasn't able to retrieve MidiDevice");
                System.exit(1);
            }

            Receiver r = new DumpReceiver();
            try {
                Transmitter t = inputDevice.getTransmitter();
                t.setReceiver(r);
            } catch (MidiUnavailableException e) {
                logger.log(Level.DEBUG, e);
            }
            logger.log(Level.DEBUG, "now running; interupt the program with [ENTER] when finished");

            try {
                System.in.read();
            } catch (IOException ioe) {
            }
            inputDevice.close();

//             logger.log(Level.DEBUG, "Received "+((DumpReceiver) r).seCount+" sysex messages with a total of "+((DumpReceiver) r).seByteCount+" bytes");
//             logger.log(Level.DEBUG, "Received "+((DumpReceiver) r).smCount+" short messages.");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.log(Level.DEBUG, e);
            }
        } catch (Throwable t) {
            logger.log(Level.DEBUG, t);
        }
        System.exit(0);
    }

    private static void printUsageAndExit() {
        System.err.println("MidiInDump: usage:");
        System.err.println("  java MidiInDump -h");
        System.err.println("    gives help information");
        System.err.println("  java MidiInDump -l");
        System.err.println("    lists available MIDI devices");
        System.err.println("  java MidiInDump [-D] -d <input device name>");
        System.err.println("    -d <input device name>\treads from named device (see '-l')");
        System.err.println("    -D\tenables debugging output");
        System.exit(1);
    }

    private static void listDevicesAndExit() {
        logger.log(Level.DEBUG, "Available MIDI Devices:");

        MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < aInfos.length; i++) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(aInfos[i]);
                boolean bAllowsInput = (device.getMaxTransmitters() != 0);
                boolean bAllowsOutput = (device.getMaxReceivers() != 0);
                //if (bAllowsInput)
                {
                    logger.log(Level.DEBUG, i + "  " + (bAllowsInput ? "IN " : "   ") +
                            (bAllowsOutput ? "OUT " : "    ") +
                            aInfos[i].getName() + ", " + aInfos[i].getVendor() +
                            ", " + aInfos[i].getVersion() + ", " +
                            aInfos[i].getDescription());
                }
            } catch (MidiUnavailableException e) {
                logger.log(Level.DEBUG, e);
            }
        }
        if (aInfos.length == 0) {
            logger.log(Level.DEBUG, "[No devices available]");
        }
        System.exit(1);
    }

    /**
     * This method tries to return a MidiDevice.Info whose name
     * matches the passed name. If no matching MidiDevice.Info is
     * found, null is returned.
     * If forOutput is true, then only output devices are searched,
     * otherwise only input devices.
     */
    private static MidiDevice.Info getMidiDeviceInfo(String strDeviceName, boolean forOutput) {
        MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();

//        logger.log(Level.DEBUG, "Searching '" + strDeviceName + "' for " + (forOutput ? "output" : "input"));
        for (MidiDevice.Info aInfo : aInfos) {
            if (aInfo.getName().equals(strDeviceName)) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(aInfo);
                    boolean bAllowsInput = (device.getMaxTransmitters() != 0);
                    boolean bAllowsOutput = (device.getMaxReceivers() != 0);

//                    logger.log(Level.DEBUG, "Looking for at '" + aInfos[i].getName() + "' with " + dev.getMaxReceivers() + " receivers and " + dev.getMaxTransmitters() + " transmitters.");
                    if ((bAllowsOutput && forOutput) || (bAllowsInput && !forOutput)) {
                        return aInfo;
                    }
                } catch (MidiUnavailableException mue) {
                }
            }
        }
        return null;
    }
}
