package jse;
/*
 *        MidiInDump.java
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
import javax.sound.midi.Transmitter;
import javax.sound.midi.Receiver;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import java.io.IOException;

/*        If the compilation fails because this class is not available,
        get gnu.getopt from the URL given in the comment below.
*/
import gnu.getopt.Getopt;


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
public class MidiInDump {
    /**        Flag for debugging messages.
             If true, some messages are dumped to the console
             during operation.
     */
    private static boolean DEBUG = true;

    public static void main(String[] args) {
        try {
            /*
             *        The device name to listen to.
             */
            String strDeviceName = null;

            /*
             *        Parsing of command-line options takes place...
             */
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
                    if (DEBUG) {
                        out("MidiInDump.main(): device name: " + strDeviceName);
                    }
                    break;
                case 'D':
                    DEBUG = true;
                    break;
                case '?':
                    printUsageAndExit();
                default:
                    out("MidiInDump.main(): getopt() returned " + c);
                    break;
                }
            }

            if (strDeviceName == null) {
                out("device name not specified!");
                printUsageAndExit();
            }

            MidiDevice.Info info = getMidiDeviceInfo(strDeviceName, false);
            if (info == null) {
                out("no device info found for name " + strDeviceName);
                System.exit(1);
            }

            MidiDevice inputDevice = null;
            try {
                inputDevice = MidiSystem.getMidiDevice(info);
                inputDevice.open();
            } catch (MidiUnavailableException e) {
                if (DEBUG) {
                    out(e);
                }
            }
            if (inputDevice == null) {
                out("wasn't able to retrieve MidiDevice");
                System.exit(1);
            }

            Receiver r = new DumpReceiver(System.out);
            try {
                Transmitter t = inputDevice.getTransmitter();
                t.setReceiver(r);
            } catch (MidiUnavailableException e) {
                if (DEBUG) {
                    out(e);
                }
            }
            out("now running; interupt the program with [ENTER] when finished");

            try {
                System.in.read();
            } catch (IOException ioe) {
            }
            inputDevice.close();

//             out("Received "+((DumpReceiver) r).seCount+" sysex messages with a total of "+((DumpReceiver) r).seByteCount+" bytes");
//             out("Received "+((DumpReceiver) r).smCount+" short messages.");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                if (DEBUG) {
                    out(e);
                }
            }
        } catch (Throwable t) {
            out(t);
        }
        System.exit(0);
    }

    private static void printUsageAndExit() {
        out("MidiInDump: usage:");
        out("  java MidiInDump -h");
        out("    gives help information");
        out("  java MidiInDump -l");
        out("    lists available MIDI devices");
        out("  java MidiInDump [-D] -d <input device name>");
        out("    -d <input device name>\treads from named device (see '-l')");
        out("    -D\tenables debugging output");
        System.exit(1);
    }

    private static void listDevicesAndExit() {
        out("Available MIDI Devices:");

        MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < aInfos.length; i++) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(aInfos[i]);
                boolean bAllowsInput = (device.getMaxTransmitters() != 0);
                boolean bAllowsOutput = (device.getMaxReceivers() != 0);
                //if (bAllowsInput)
                {
                    out("" + i + "  " + (bAllowsInput ? "IN " : "   ") +
                        (bAllowsOutput ? "OUT " : "    ") +
                        aInfos[i].getName() + ", " + aInfos[i].getVendor() +
                        ", " + aInfos[i].getVersion() + ", " +
                        aInfos[i].getDescription());
                }
            } catch (MidiUnavailableException e) {
                out(e);
            }
        }
        if (aInfos.length == 0) {
            out("[No devices available]");
        }
        System.exit(1);
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

    private static void out(String strMessage) {
        System.out.println(strMessage);
    }

    private static void out(Throwable t) {
        t.printStackTrace();
    }
}

/* */
