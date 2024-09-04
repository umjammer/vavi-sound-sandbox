/*
 *  Copyright (c) 1999, 2000 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
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

package jse;

import java.io.File;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URL;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;

import static java.lang.System.getLogger;


/*        +DocBookXML
        <title>Getting information about a MIDI file</title>

        <formalpara><title>Purpose</title>
        <para>Displays general information about a MIDI file:
        MIDI file type, division type, timing resolution, length (in ticks)
        and duration (in &mu;s)</para>
        </formalpara>

        <formalpara><title>Level</title>
        <para>newbie</para></formalpara>

        <formalpara><title>Usage</title>
        <para>
        <cmdsynopsis><command>java MidiFileInfo</command>
        <group>
        <arg><option>-f</option></arg>
        <arg><option>-u</option></arg>
        <arg><option>-s</option></arg>
        </group>
        <arg><option>-i</option></arg>
        <arg><replaceable>midifile</replaceable></arg>
        </cmdsynopsis>
        </para>
        </formalpara>

        <formalpara><title>Parameters</title>
        <variablelist>
        <varlistentry>
        <term><option>-s</option></term>
        <listitem><para>use standard input as source for the MIDI file. If this option is given, <replaceable class="parameter">midifile</replaceable> is not required.</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-f</option></term>
        <listitem><para>interpret <replaceable class="parameter">midifile</replaceable> as filename. This is the default. If this option is given, <replaceable class="parameter">midifile</replaceable> is required.</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-u</option></term>
        <listitem><para>interpret <replaceable class="parameter">midifile</replaceable> as URL. If this option is given, <replaceable class="parameter">midifile</replaceable> is required.</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-i</option></term>
        <listitem><para>display additional information</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><replaceable class="parameter">midifile</replaceable></term>
        <listitem><para>the filename  or URL of the MIDI
        file that should be used</para></listitem>
        </varlistentry>
        </variablelist>
        </formalpara>

        <formalpara><title>Bugs, limitations</title>
        <para>
        Some combination of options do not work. With Sun's
        implementation tick and microsecond length are only shown with option <option>-i</option>. With <ulink url="http://www.tritonus.org/">Tritonus</ulink>, length and duration cannot be displayed at all.
        </para></formalpara>

        <formalpara><title>Source code</title>
        <para>
        <ulink url="MidiFileInfo.java.html">MidiFileInfo.java</ulink>
        </para>
        </formalpara>

-DocBookXML
*/

/**
 * MidiFileInfo.java
 * <p>
 * This file is part of the Java Sound Examples.
 */
public class MidiFileInfo {

    private static final Logger logger = getLogger(MidiFileInfo.class.getName());

    private static final int LOAD_METHOD_STREAM = 1;
    private static final int LOAD_METHOD_FILE = 2;
    private static final int LOAD_METHOD_URL = 3;

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsageAndExit();
        }

        int nLoadMethod = LOAD_METHOD_FILE;
        boolean bCheckSequence = false;
        int nCurrentArg = 0;
        while (nCurrentArg < args.length) {
            switch (args[nCurrentArg]) {
                case "-h" -> printUsageAndExit();
                case "-s" -> nLoadMethod = LOAD_METHOD_STREAM;
                case "-f" -> nLoadMethod = LOAD_METHOD_FILE;
                case "-u" -> nLoadMethod = LOAD_METHOD_URL;
                case "-i" -> bCheckSequence = true;
            }

            nCurrentArg++;
        }

        String strSource = args[nCurrentArg - 1];
        String strFilename = null;
        MidiFileFormat fileFormat = null;
        Sequence sequence = null;
        try {
            switch (nLoadMethod) {
                case LOAD_METHOD_STREAM:

                    InputStream inputStream = System.in;
                    fileFormat = MidiSystem.getMidiFileFormat(inputStream);
                    strFilename = "<standard input>";
                    if (bCheckSequence) {
                        sequence = MidiSystem.getSequence(inputStream);
                    }
                    break;
                case LOAD_METHOD_FILE:

                    File file = new File(strSource);
                    fileFormat = MidiSystem.getMidiFileFormat(file);
                    strFilename = file.getCanonicalPath();
                    if (bCheckSequence) {
                        sequence = MidiSystem.getSequence(file);
                    }
                    break;
                case LOAD_METHOD_URL:

                    URL url = new URL(strSource);
                    fileFormat = MidiSystem.getMidiFileFormat(url);
                    strFilename = url.toString();
                    if (bCheckSequence) {
                        sequence = MidiSystem.getSequence(url);
                    }
                    break;
            }
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            System.exit(1);
        }

        /*
         *        And now, we output the data.
         */
        if (fileFormat == null) {
            System.out.println("Cannot determine format");
        } else {
            System.out.println("---------------------------------------------------------------------------");
            System.out.println("Source: " + strFilename);
            System.out.println("Midi File Type: " + fileFormat.getType());

            float fDivisionType = fileFormat.getDivisionType();
            String strDivisionType = null;
            if (fDivisionType == Sequence.PPQ) {
                strDivisionType = "PPQ";
            } else if (fDivisionType == Sequence.SMPTE_24) {
                strDivisionType = "SMPTE, 24 frames per second";
            } else if (fDivisionType == Sequence.SMPTE_25) {
                strDivisionType = "SMPTE, 25 frames per second";
            } else if (fDivisionType == Sequence.SMPTE_30DROP) {
                strDivisionType = "SMPTE, 29.97 frames per second";
            } else if (fDivisionType == Sequence.SMPTE_30) {
                strDivisionType = "SMPTE, 30 frames per second";
            }

            System.out.println("DivisionType: " + strDivisionType);

            String strResolutionType = null;
            if (fileFormat.getDivisionType() == Sequence.PPQ) {
                strResolutionType = " ticks per beat";
            } else {
                strResolutionType = " ticks per frame";
            }
            System.out.println("Resolution: " + fileFormat.getResolution() +
                    strResolutionType);

            String strFileLength = null;
            if (fileFormat.getByteLength() != MidiFileFormat.UNKNOWN_LENGTH) {
                strFileLength = fileFormat.getByteLength() + " bytes";
            } else {
                strFileLength = "unknown";
            }
            System.out.println("Length: " + strFileLength);

            String strDuration = null;
            if (fileFormat.getMicrosecondLength() != MidiFileFormat.UNKNOWN_LENGTH) {
                strDuration = fileFormat.getMicrosecondLength() +
                        " microseconds)";
            } else {
                strDuration = "unknown";
            }
            System.out.println("Duration: " + strDuration);

            if (bCheckSequence) {
                System.out.println("[Sequence says:] Length: " +
                        sequence.getTickLength() + " ticks (= " +
                        sequence.getMicrosecondLength() + " us)");
            }
            System.out.println("---------------------------------------------------------------------------");
        }
    }

    private static void printUsageAndExit() {
        System.out.println("MidiFileInfo: usage:");
        System.out.println("\tjava MidiFileInfo [-s|-f|-u] [-i] <midifile>");
        System.exit(1);
    }
}
