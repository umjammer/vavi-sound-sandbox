/*
 * Copyright (c) 1999 - 2001 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
 *
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package jse;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;

import gnu.getopt.Getopt;

import static java.lang.System.getLogger;


/*
 * +DocBookXML <title>Playing a MIDI file (advanced)</title>
 *
 * <formalpara><title>Purpose</title> <para>Plays a single MIDI file. Allows
 * to select the sequencer, the synthesizer or MIDI port or dumping to the
 * console.</para> </formalpara>
 *
 * <formalpara><title>Level</title> <para>advanced</para></formalpara>
 *
 * <formalpara><title>Usage</title> <para> <cmdsynopsis> <command>java
 * MidiPlayer</command> <arg choice="plain"><option>-l</option></arg>
 * </cmdsynopsis> <cmdsynopsis> <command>java MidiPlayer</command> <arg><option>-s</option></arg>
 * <arg><option>-m</option></arg> <arg><option>-d <replaceable>devicename</replaceable></option></arg>
 * <arg><option>-c</option></arg> <arg><option>-S <replaceable>sequencername</replaceable></option></arg>
 * <arg choice="plain"><replaceable>midifile</replaceable></arg>
 * </cmdsynopsis> </para></formalpara>
 *
 * <formalpara><title>Parameters</title> <variablelist> <varlistentry> <term><option>-l</option></term>
 * <listitem><para>list the availabe MIDI devices, including sequencers</para></listitem>
 * </varlistentry> <varlistentry> <term><option>-m</option></term> <listitem><para>play
 * on the MIDI port</para></listitem> </varlistentry> <varlistentry> <term><option>-d
 * <replaceable>devicename</replaceable></option></term> <listitem><para>play
 * on the named MIDI device</para></listitem> </varlistentry> <varlistentry>
 * <term><option>-c</option></term> <listitem><para>dump on the console</para></listitem>
 * </varlistentry> <varlistentry>
 *
 * <term><option>-S <replaceable>sequencername</replaceable></option></term>
 * <listitem><para>play using the named Sequencer</para></listitem>
 * </varlistentry> <varlistentry> <term><option><replaceable>midifile</replaceable></option></term>
 * <listitem><para>the name of the MIDI file that should be played</para></listitem>
 * </varlistentry> </variablelist>
 *
 * <para>All options may be used together. No option is equal to giving
 * <option>-s</option>.</para>
 *
 * </formalpara>
 *
 * <formalpara><title>Bugs, limitations</title> <para> For the Sun jdk1.3,
 * playing to the MIDI port and dumping to the console do not work. You can make
 * it work by installing the JavaSequencer plug-in from<ulink url
 * ="http://www.tritonus.org/plugins.html">Tritonus Plug-ins</ulink>.</para>
 *
 * <para>For Tritonus, playing RMF files does not work (and will not work until
 * the specs are published). </para> </formalpara>
 *
 * <formalpara><title>Source code</title> <para> <ulink
 * url="MidiPlayer.java.html">MidiPlayer.java</ulink>, <ulink
 * url="DumpReceiver.java.html">DumpReceiver.java</ulink>, <ulink
 * url="http://www.urbanophile.com/arenn/hacking/download.html">gnu.getopt.Getopt</ulink>
 * </para> </formalpara>
 *
 * -DocBookXML
 *
 * MidiPlayer
 *
 * This file is part of the Java Sound Examples.
 */
public class MidiPlayer {

    private static final Logger logger = getLogger(MidiPlayer.class.getName());

    private static Sequencer sm_sequencer = null;

    /**
     * List of opened MidiDevices. This stores references to all MidiDevices
     * that we've opened except the sequencer. It is used to close them properly
     * on exit.
     */
    private static List<MidiDevice> sm_openedMidiDeviceList;

    public static void main(String[] args) {
        /*
         * Set when the sequence should be played on the default internal
         * synthesizer.
         */
        boolean bUseSynthesizer = false;

        /*
         * Set when the sequence should be played on the default external MIDI
         * port.
         */
        boolean bUseMidiPort = false;

        /*
         * Set when the sequence should be played on a MidiDevice whose name is
         * in strDeviceName. This can be any device, including internal or
         * external synthesizers, MIDI ports or even sequencers.
         */
        boolean bUseDevice = false;

        /*
         * Set when the sequence should be dumped in the console window (or
         * whereever the standard output is routed to). This gives detailed
         * information about each MIDI event.
         */
        boolean bUseConsoleDump = false;

        /*
         * The device name to use when bUseDevice is set.
         */
        String strDeviceName = null;

        /*
         * The name of the sequencer to use. This is optional. If not set, the
         * default sequencer is used.
         */
        String strSequencerName = null;

        /*
         * Parsing of command-line options takes place...
         */
        Getopt g = new Getopt("MidiPlayer", args, "hlsmd:cS:D");
        int c;
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'h':
                    printUsageAndExit();
                case 'l':
                    listDevicesAndExit(false, true);
                case 's':
                    bUseSynthesizer = true;
                    break;
                case 'm':
                    bUseMidiPort = true;
                    break;
                case 'd':
                    bUseDevice = true;
                    strDeviceName = g.getOptarg();
                    logger.log(Level.DEBUG, "MidiPlayer.main(): device name: " + strDeviceName);
                    break;
                case 'c':
                    bUseConsoleDump = true;
                    break;
                case 'S':
                    strSequencerName = g.getOptarg();
                    logger.log(Level.DEBUG, "MidiPlayer.main(): sequencer name: " + strSequencerName);
                    break;
                case '?':
                    printUsageAndExit();
                default:
                    System.out.println("getopt() returned " + c);
                    break;
            }
        }

        // If no destination option is choosen at all, we default to playing on
        // the internal synthesizer.
        if (!(bUseSynthesizer | bUseMidiPort | bUseDevice | bUseConsoleDump)) {
            bUseSynthesizer = true;
        }

        // We make shure that there is only one more argument, which we take as
        // the filename of the MIDI file we want to play.
        String strFilename = null;
        for (int i = g.getOptind(); i < args.length; i++) {
            if (strFilename == null) {
                strFilename = args[i];
            } else {
                printUsageAndExit();
            }
        }
        if (strFilename == null) {
            printUsageAndExit();
        }

        File midiFile = new File(strFilename);

        /*
         * We create an (File)InputStream and decorate it with a buffered
         * stream. This is set later at the Sequencer as the source of a
         * sequence.
         *
         * There is another programming technique: Creating a Sequence object
         * from the file and set this at the Sequencer. While this technique
         * seems more natural, it in fact is less efficient on Sun's
         * implementation of the Java Sound API. Furthermore, it sucks for RMF
         * files. So for now, I consider the technique used here as the
         * "official" one. But note that this depends on facts that are
         * implementation-dependant; it is only true for the Sun implementation.
         * In Tritonus, efficiency is the other way round. (And Tritonus has no
         * RMF support because the specs are proprietary.)
         */
        InputStream sequenceStream = null;
        try {
            sequenceStream = new FileInputStream(midiFile);
            sequenceStream = new BufferedInputStream(sequenceStream, 1024);
        } catch (IOException e) {
            /*
             * In case of an exception, we dump the exception including the
             * stack trace to the console output. Then, we exit the program.
             */
            logger.log(Level.ERROR, e.getMessage(), e);
            System.exit(1);
        }

        /*
         * Now, we need a Sequencer to play the sequence. In case we have passed
         * a sequencer name on the command line, we try to get that specific
         * sequencer. Otherwise, we simply request the default sequencer.
         */
        try {
            if (strSequencerName != null) {
                MidiDevice.Info seqInfo = getMidiDeviceInfo(strSequencerName, true);
                if (seqInfo == null) {
                    System.out.println("Cannot find device " + strSequencerName);
                    System.exit(1);
                }
                sm_sequencer = (Sequencer) MidiSystem.getMidiDevice(seqInfo);
            } else {
                sm_sequencer = MidiSystem.getSequencer();
            }
        } catch (MidiUnavailableException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            System.exit(1);
        }
        if (sm_sequencer == null) {
            System.out.println("MidiPlayer.main(): can't get a Sequencer");
            System.exit(1);
        }

        /*
         * There is a bug in the Sun jdk1.3. It prevents correct termination of
         * the VM. So we have to exit ourselves. To accomplish this, we register
         * a Listener to the Sequencer. It is called when there are "meta"
         * events. Meta event 47 is end of track.
         *
         * Thanks to Espen Riskedal for finding this trick.
         */
        sm_sequencer.addMetaEventListener(event -> {
            if (event.getType() == 47) {
                logger.log(Level.DEBUG, "MidiPlayer.<...>.meta(): end of track message received, closing sequencer and attached MidiDevices...");
                sm_sequencer.close();

                for (MidiDevice device : sm_openedMidiDeviceList) {
                    device.close();
                }
                logger.log(Level.DEBUG, "MidiPlayer.<...>.meta(): ...closed, now exiting");
                System.exit(0);
            }
        });

        // If we are in debug mode, we set additional listeners to produce
        // interesting (?) debugging output.
        if (logger.isLoggable(Level.DEBUG)) {
            sm_sequencer.addMetaEventListener(message -> {
                logger.log(Level.DEBUG, "%%% MetaMessage: " + message);
                logger.log(Level.DEBUG, "%%% MetaMessage type: " + message.getType());
                logger.log(Level.DEBUG, "%%% MetaMessage length: " + message.getLength());
            });

            sm_sequencer.addControllerEventListener(message -> {
                logger.log(Level.DEBUG, "%%% ShortMessage: " + message);
                logger.log(Level.DEBUG, "%%% ShortMessage controller: " + message.getData1());
                logger.log(Level.DEBUG, "%%% ShortMessage value: " + message.getData2());
            }, null);
        }

        /*
         * The Sequencer is still a dead object. We have to open() it to become
         * live. This is necessary to allocate some ressources in the native
         * part.
         */
        try {
            sm_sequencer.open();
        } catch (MidiUnavailableException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            System.exit(1);
        }

        /*
         * Next step is to tell the Sequencer which Sequence it has to play. In
         * this case, we set it as the InputStream created above.
         */
        try {
            sm_sequencer.setSequence(sequenceStream);
        } catch (InvalidMidiDataException | IOException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            System.exit(1);
        }

        // Now, we set up the destinations the Sequence should be
        // played on.
        sm_openedMidiDeviceList = new ArrayList<>();
        if (bUseSynthesizer) {
            // We try to get the default synthesizer, open()
            // it and chain it to the sequencer with a
            // Transmitter-Receiver pair.
            try {
                Synthesizer synth = MidiSystem.getSynthesizer();
                synth.open();
                sm_openedMidiDeviceList.add(synth);

                Receiver synthReceiver = synth.getReceiver();
                Transmitter seqTransmitter = sm_sequencer.getTransmitter();
                seqTransmitter.setReceiver(synthReceiver);
            } catch (MidiUnavailableException e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
        }

        if (bUseMidiPort) {
            // We try to get a Receiver which is already
            // associated with the default MIDI port.
            // It is then linked to a sequencer's
            // Transmitter.
            try {
                Receiver midiReceiver = MidiSystem.getReceiver();
                Transmitter midiTransmitter = sm_sequencer.getTransmitter();
                midiTransmitter.setReceiver(midiReceiver);
            } catch (MidiUnavailableException e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
        }

        if (bUseDevice) {
            // Here, we try to use a MidiDevice as destination
            // whose name was passed on the command line.
            // It is then linked to a sequencer's
            // Transmitter.
            // MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();
            MidiDevice.Info info = getMidiDeviceInfo(strDeviceName, true);
            if (info == null) {
                System.out.println("Cannot find device " + strDeviceName);
            }
            try {
                MidiDevice midiDevice = MidiSystem.getMidiDevice(info);
                midiDevice.open();
                sm_openedMidiDeviceList.add(midiDevice);

                Receiver midiReceiver = midiDevice.getReceiver();
                Transmitter midiTransmitter = sm_sequencer.getTransmitter();
                midiTransmitter.setReceiver(midiReceiver);
            } catch (MidiUnavailableException e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
        }

        if (bUseConsoleDump) {
            /*
             * We allocate a DumpReceiver object. Its job is to print
             * information on all received events to the console. It is then
             * linked to a sequencer's Transmitter.
             */
            try {
                Receiver dumpReceiver = new DumpReceiver();
                Transmitter dumpTransmitter = sm_sequencer.getTransmitter();
                dumpTransmitter.setReceiver(dumpReceiver);
            } catch (MidiUnavailableException e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
        }

        /*
         * Now, we can start over.
         */
        logger.log(Level.DEBUG, "MidiPlayer.main(): starting sequencer...");
        sm_sequencer.start();
        logger.log(Level.DEBUG, "MidiPlayer.main(): ...started");
    }

    private static void printUsageAndExit() {
        System.out.println("MidiPlayer: usage:");
        System.out.println("  java MidiPlayer -h");
        System.out.println("    gives help information");
        System.out.println("  java MidiPlayer -l");
        System.out.println("    lists available MIDI devices");
        System.out.println("  java MidiPlayer [-s] [-m] [-d <output device name>] [-c] [-S <sequencer name>] [-D] <midifile>");
        System.out.println("    -s\tplays on the default synthesizer");
        System.out.println("    -m\tplays on the MIDI port");
        System.out.println("    -d <output device name>\toutputs to named device (see '-l')");
        System.out.println("    -c\tdumps to the console");
        System.out.println("    -S <sequencer name>\tuses named sequencer (see '-l')");
        System.out.println("    -D\tenables debugging output");
        System.out.println("All options may be used together.");
        System.out.println("No option is equal to giving -s.");
        System.exit(1);
    }

    private static void listDevicesAndExit(boolean forInput, boolean forOutput) {
        if (forInput && !forOutput) {
            logger.log(Level.DEBUG, "Available MIDI IN Devices:");
        } else if (!forInput && forOutput) {
            logger.log(Level.DEBUG, "Available MIDI OUT Devices:");
        } else {
            logger.log(Level.DEBUG, "Available MIDI Devices:");
        }

        MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < aInfos.length; i++) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(aInfos[i]);
                boolean bAllowsInput = (device.getMaxTransmitters() != 0);
                boolean bAllowsOutput = (device.getMaxReceivers() != 0);
                if ((bAllowsInput && forInput) || (bAllowsOutput && forOutput)) {
                    logger.log(Level.DEBUG, i + "  " + (bAllowsInput ? "IN " : "   ") + (bAllowsOutput ? "OUT " : "    ") + new String(aInfos[i].getName().getBytes("ISO8859-1")) + ", " + aInfos[i].getVendor() + ", " + aInfos[i].getVersion() + ", " + aInfos[i].getDescription());
                }
            } catch (MidiUnavailableException e) {
                // device is obviously not available...
            } catch (IOException e) {
                System.err.println(e);

                // device is obviously not available...
            }
        }
        if (aInfos.length == 0) {
            logger.log(Level.DEBUG, "[No devices available]");
        }
        System.exit(0);
    }

    /**
     * This method tries to return a MidiDevice.Info whose name matches the
     * passed name. If no matching MidiDevice.Info is found, null is returned.
     * If forOutput is true, then only output devices are searched, otherwise
     * only input devices.
     */
    private static MidiDevice.Info getMidiDeviceInfo(String strDeviceName, boolean forOutput) {
        MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info aInfo : aInfos) {
            try {
                if (new String(aInfo.getName().getBytes("ISO8859-1")).equals(strDeviceName)) {
                    try {
                        MidiDevice device = MidiSystem.getMidiDevice(aInfo);
                        boolean bAllowsInput = (device.getMaxTransmitters() != 0);
                        boolean bAllowsOutput = (device.getMaxReceivers() != 0);
                        if ((bAllowsOutput && forOutput) || (bAllowsInput && !forOutput)) {
                            return aInfo;
                        }
                    } catch (MidiUnavailableException mue) {
                    }
                }
            } catch (IOException e) {
            }
        }
        return null;
    }
}
