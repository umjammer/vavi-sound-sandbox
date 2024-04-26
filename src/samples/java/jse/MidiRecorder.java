
package jse;

/*
 *        MidiRecorder.java
 *
 *        This file is part of the Java Sound Examples.
 */
/*
 *  Copyright (c) 1999 - 2001 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
 *
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
import gnu.getopt.Getopt;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;


/*
 * +DocBookXML <title>Plays a single MIDI or RMF file</title>
 *
 * <formalpara><title>Purpose</title> <para>Plays a single MIDI or RMF file.</para></formalpara>
 *
 * <formalpara><title>Level</title> <para>Command-line program</para></formalpara>
 *
 * <formalpara><title>Usage</title> <para> <synopsis>java MidiRecorder -l</synopsis>
 * <synopsis>java MidiRecorder [-s] [-m] [-d &lt;devicename&gt;] [-c] [-S
 * &lt;sequencername&gt;] &lt;midifile&gt;</synopsis> </para></formalpara>
 *
 * <formalpara><title>Parameters</title> <variablelist> <varlistentry> <term><option>-l</option></term>
 * <listitem><para>list the availabe MIDI devices</para></listitem>
 * </varlistentry> <varlistentry> <term><option>-m</option></term> <listitem><para>play
 * on the MIDI port</para></listitem> </varlistentry> <varlistentry> <term><option>-d
 * &lt;devicename&gt;</option></term> <listitem><para>play on the named MIDI
 * device</para></listitem> </varlistentry> <varlistentry> <term><option>-c</option></term>
 * <listitem><para>dump on the console</para></listitem>
 *
 * All options may be used together. No option is equal to giving <option>-s</option>.
 *
 * </varlistentry> <varlistentry> <term><option>-S &lt;sequencername&gt;</option></term>
 * <listitem><para>play using the named Sequencer</para></listitem>
 * </varlistentry> <varlistentry> <term><option>&lt;midifile&gt;</option></term>
 * <listitem><para>the file name of the MIDI or RMF file that should be played</para></listitem>
 * </varlistentry> </variablelist> </formalpara>
 *
 * <formalpara><title>Bugs, limitations</title> <para> For the Sun jdk1.3,
 * playing to the MIDI port and dumping to the console do not work. You can make
 * it work by installing the <ulink url
 * ="http://www.tritonus.org/plugins.html">MidiShare plug-in</ulink>. For
 * Tritonus, playing RMF files does not work (and will not work until the specs
 * are published). </para></formalpara>
 *
 * <formalpara><title>Source code</title> <para> <ulink
 * url="MidiRecorder.java.html">MidiRecorder.java</ulink>, <ulink
 * url="DumpReceiver.java.html">DumpReceiver.java</ulink>, <ulink
 * url="http://www.urbanophile.com/arenn/hacking/download.html">gnu.getopt.Getopt</ulink>
 * </para></formalpara>
 *
 * -DocBookXML
 */
public class MidiRecorder {
    /**
     * Flag for debugging messages. If true, some messages are dumped to the
     * console during operation.
     */
    private static boolean DEBUG = false;

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
        Getopt g = new Getopt("MidiRecorder", args, "hlsmd:cS:D");
        int c;
        while ((c = g.getopt()) != -1) {
            switch (c) {
            case 'h':
                printUsageAndExit();
            case 'l':
                listDevicesAndExit(true, false);
            case 's':
                bUseSynthesizer = true;
                break;
            case 'm':
                bUseMidiPort = true;
                break;
            case 'd':
                bUseDevice = true;
                strDeviceName = g.getOptarg();
                if (DEBUG) {
                    System.out.println("MidiRecorder.main(): device name: " + strDeviceName);
                }
                break;
            case 'c':
                bUseConsoleDump = true;
                break;
            case 'S':
                strSequencerName = g.getOptarg();
                if (DEBUG) {
                    System.out.println("MidiRecorder.main(): sequencer name: " + strSequencerName);
                }
                break;
            case 'D':
                DEBUG = true;
                break;
            case '?':
                printUsageAndExit();
            default:
                System.out.println("getopt() returned " + c);
                break;
            }
        }

        /*
         * If no destination option is choosen at all, we default to playing on
         * the internal synthesizer.
         */
        if (!(bUseSynthesizer | bUseMidiPort | bUseDevice | bUseConsoleDump)) {
            bUseSynthesizer = true;
        }

        /*
         * We make shure that there is only one more argument, which we take as
         * the filename of the MIDI file we want to play.
         */
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
            e.printStackTrace();
            System.exit(1);
        }

        /*
         * Now, we need a Sequencer to play the sequence. In case we have passed
         * a sequencer name on the command line, we try to get that specific
         * sequencer. Otherwise, we simply request the default sequencer.
         */
        try {
            if (strSequencerName != null) {
                MidiDevice.Info seqInfo = getMidiDeviceInfo(strSequencerName, false);
                if (seqInfo == null) {
                    System.out.println("Cannot find device " + strSequencerName);
                    System.exit(1);
                }
                sm_sequencer = (Sequencer) MidiSystem.getMidiDevice(seqInfo);
            } else {
                sm_sequencer = MidiSystem.getSequencer();
            }
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
            System.exit(1);
        }
        if (sm_sequencer == null) {
            System.out.println("MidiRecorder.main(): can't get a Sequencer");
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
        sm_sequencer.addMetaEventListener(new MetaEventListener() {
            public void meta(MetaMessage event) {
                if (event.getType() == 47) {
                    if (DEBUG) {
                        out("MidiRecorder.<...>.meta(): end of track message received, closing sequencer and attached MidiDevices...");
                    }
                    sm_sequencer.close();

                    Iterator<MidiDevice> iterator = sm_openedMidiDeviceList.iterator();
                    while (iterator.hasNext()) {
                        MidiDevice device = iterator.next();
                        device.close();
                    }
                    if (DEBUG) {
                        out("MidiRecorder.<...>.meta(): ...closed, now exiting");
                    }
                    System.exit(0);
                }
            }
        });

        /*
         * If we are in debug mode, we set additional listeners to produce
         * interesting (?) debugging output.
         */
        if (DEBUG) {
            sm_sequencer.addMetaEventListener(new MetaEventListener() {
                public void meta(MetaMessage message) {
                    System.out.println("%%% MetaMessage: " + message);
                    System.out.println("%%% MetaMessage type: " + message.getType());
                    System.out.println("%%% MetaMessage length: " + message.getLength());
                }
            });

            sm_sequencer.addControllerEventListener(new ControllerEventListener() {
                public void controlChange(ShortMessage message) {
                    System.out.println("%%% ShortMessage: " + message);
                    System.out.println("%%% ShortMessage controller: " + message.getData1());
                    System.out.println("%%% ShortMessage value: " + message.getData2());
                }
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
            e.printStackTrace();
            System.exit(1);
        }

        /*
         * Next step is to tell the Sequencer which Sequence it has to play. In
         * this case, we set it as the InputStream created above.
         */
        try {
            sm_sequencer.setSequence(sequenceStream);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        /*
         * Now, we set up the destinations the Sequence should be played on.
         */
        sm_openedMidiDeviceList = new ArrayList<>();
        if (bUseSynthesizer) {
            /*
             * We try to get the default synthesizer, open() it and chain it to
             * the sequencer with a Transmitter-Receiver pair.
             */
            try {
                Synthesizer synth = MidiSystem.getSynthesizer();
                synth.open();
                sm_openedMidiDeviceList.add(synth);

                Receiver synthReceiver = synth.getReceiver();
                Transmitter seqTransmitter = sm_sequencer.getTransmitter();
                seqTransmitter.setReceiver(synthReceiver);
            } catch (MidiUnavailableException e) {
                e.printStackTrace();
            }
        }

        if (bUseMidiPort) {
            /*
             * We try to get a Receiver which is already associated with the
             * default MIDI port. It is then linked to a sequencer's
             * Transmitter.
             */
            try {
                Receiver midiReceiver = MidiSystem.getReceiver();
                Transmitter midiTransmitter = sm_sequencer.getTransmitter();
                midiTransmitter.setReceiver(midiReceiver);
            } catch (MidiUnavailableException e) {
                e.printStackTrace();
            }
        }

        if (bUseDevice) {
            /*
             * Here, we try to use a MidiDevice as destination whose name was
             * passed on the command line. It is then linked to a sequencer's
             * Transmitter.
             */
//          MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();
            MidiDevice.Info info = getMidiDeviceInfo(strDeviceName, false);
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
                e.printStackTrace();
            }
        }

        if (bUseConsoleDump) {
            /*
             * We allocate a DumpReceiver object. Its job is to print
             * information on all received events to the console. It is then
             * linked to a sequencer's Transmitter.
             */
            try {
                Receiver dumpReceiver = new DumpReceiver(System.out);
                Transmitter dumpTransmitter = sm_sequencer.getTransmitter();
                dumpTransmitter.setReceiver(dumpReceiver);
            } catch (MidiUnavailableException e) {
                e.printStackTrace();
            }
        }

        /*
         * Now, we can start over.
         */
        if (DEBUG) {
            out("MidiRecorder.main(): starting sequencer...");
        }
        sm_sequencer.start();
        if (DEBUG) {
            out("MidiRecorder.main(): ...started");
        }
    }

    private static void printUsageAndExit() {
        System.out.println("MidiRecorder: usage:");
        System.out.println("  java MidiRecorder -h");
        System.out.println("    gives help information");
        System.out.println("  java MidiRecorder -l");
        System.out.println("    lists available MIDI devices");
        System.out.println("  java MidiRecorder [-s] [-m] [-d <output device name>] [-c] [-S <sequencer name>] [-D] <midifile>");
        System.out.println("    -s\tplays on the internal synthesizer");
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
                    out("" + i + "  " + (bAllowsInput ? "IN " : "   ") + (bAllowsOutput ? "OUT " : "    ") + aInfos[i].getName() + ", " + aInfos[i].getVendor() + ", " + aInfos[i].getVersion() + ", " + aInfos[i].getDescription());
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
     * This method tries to return a MidiDevice.Info whose name matches the
     * passed name. If no matching MidiDevice.Info is found, null is returned.
     * If forOutput is true, then only output devices are searched, otherwise
     * only input devices.
     */
    private static MidiDevice.Info getMidiDeviceInfo(String strDeviceName, boolean forOutput) {
        MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < aInfos.length; i++) {
            if (aInfos[i].getName().equals(strDeviceName)) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(aInfos[i]);
                    boolean bAllowsInput = (device.getMaxTransmitters() != 0);
                    boolean bAllowsOutput = (device.getMaxReceivers() != 0);
                    if ((bAllowsOutput && forOutput) || (bAllowsInput && !forOutput)) {
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
}
