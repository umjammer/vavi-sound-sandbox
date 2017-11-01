/*
 * Copyright (c) 1999 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
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

import java.io.File;
import java.io.IOException;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;


/*        +DocBookXML
        <title>Converting MIDI type 1 files to MIDI type 0 files</title>

        <formalpara><title>Purpose</title>
        <para>This program can convert MIDI type 1 files to MIDI type 0 files.
        It has two modes: single mode and multi mode.</para>

        <para>In single mode (selected by <option>-s</option>),
        all events of all tracks are
        assembled in one track which is saved as a single file
        (not yet working).</para>

        <para>In multi mode (selected by <option>-m</option>),
        each track of the input file is separated and
        saved in its own file. The filenames of the output files are derived
        from the name of the input file. In multi mode, the track number is
        appended to the basename of the input file (i.e. it is inserted
        before the extension).</para>
        </formalpara>

        <formalpara><title>Level</title>
        <para>advanced</para>
        </formalpara>

        <formalpara><title>Usage</title>
        <para>
        <cmdsynopsis><command>java MidiConverter</command>
        <group>
        <arg><option>-s</option></arg>
        <arg><option>-m</option></arg>
        </group>
        <arg choice="plain"><replaceable class="parameter">midifile</replaceable></arg>
        </cmdsynopsis>
        </para></formalpara>

        <formalpara><title>Parameters</title>
        <variablelist>
        <varlistentry>
        <term><option>-s</option></term>
        <listitem><para>selects single mode (default)</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-m</option></term>
        <listitem><para>selects multi mode</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><replaceable class="parameter">midifile</replaceable></term>
        <listitem><para>the name of the MIDI file to process</para></listitem>
        </varlistentry>
        </variablelist>
        </formalpara>

        <formalpara><title>Bugs, limitations</title>
        <para>Not implemented completely and not well-tested.</para>
        </formalpara>

        <formalpara><title>Source code</title>
        <para>
        <ulink url="MidiConverter.java.html">MidiConverter.java</ulink>
        </para>
        </formalpara>

-DocBookXML
*/

/**
 * MidiConverter
 *
 * This file is part of the Java Sound Examples.
 */
public class MidiConverter {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("usage:");
            System.out.println("java MidiConverter  [-s | -m]  <midifile>");
            System.exit(1);
        }

        // This variable says whether we should process in
        // multi mode (each track is saved in an own file)
        // or in single mode (all tracks are united to one
        // track and saved in a single file).
        boolean bUseMultiMode = false;
        if (args[0].equals("-m")) {
            bUseMultiMode = true;
        } else if (args[0].equals("-s")) {
            bUseMultiMode = false;
//      } else {
//             System.out.println("You have to specify either single mode (-s) or multi mode (-m).");
//             System.exit(1);
         }
        String strFilename = args[1];
        Sequence sequence = null;
        try {
            sequence = MidiSystem.getSequence(new File(strFilename));
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        Track[] aTracks = sequence.getTracks();
        if (aTracks.length == 1) {
            System.out.println("The file you specified is already a type 0 MIDI file.");
            System.exit(1);
        }

        float fDivisionType = sequence.getDivisionType();
        int nResolution = sequence.getResolution();
        if (bUseMultiMode) {
            for (int nTrack = 0; nTrack < aTracks.length; nTrack++) {
                Sequence singleTrackSequence = null;
                try {
                    singleTrackSequence = new Sequence(fDivisionType, nResolution);
                } catch (InvalidMidiDataException e) {
                    e.printStackTrace();
                    System.exit(1);
                }

                Track track = singleTrackSequence.createTrack();
                for (int i = 0; i < aTracks[nTrack].size(); i++) {
                    track.add(aTracks[nTrack].get(i));
                }

                int nDotPosition = strFilename.lastIndexOf('.');
                String strSingleTrackFilename = null;
                if (nDotPosition == -1) {
                    strSingleTrackFilename = strFilename.substring(0, nDotPosition) + "-" + nTrack;
                } else {
                    strSingleTrackFilename = strFilename.substring(0, nDotPosition) + "-" + nTrack + strFilename.substring(nDotPosition);
                }

//              MidiFileFormat fileFormat = new MidiFileFormat(0, fDivisionType, nResolution, MidiFileFormat.UNKNOWN_LENGTH, MidiFileFormat.UNKNOWN_LENGTH);
                try {
                    MidiSystem.write(singleTrackSequence, 0, new File(strSingleTrackFilename));
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        } else { // single mode
        }

        // This is only necessary because of a bug in jdk1.3.
        System.exit(0);
    }
}

/* */
