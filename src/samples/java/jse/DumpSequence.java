/*
 * DumpSequence.java
 *
 * This file is part of the Java Sound Examples.
 */
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
import java.io.IOException;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;


/**
 * +DocBookXML <title>Displaying the content of a MIDI file</title>
 *
 * <formalpara><title>Purpose</title> <para>Dumps the decoded content of a MIDI file to the console.</para> </formalpara>
 *
 * <formalpara><title>Level</title> <para>advanced</para></formalpara>
 *
 * <formalpara><title>Usage</title> <para> <cmdsynopsis><command>java DumpSequence</command> <arg choice="plain"><replaceable
 * class="parameter">midifile</replaceable></arg> </cmdsynopsis> </para></formalpara>
 *
 * <formalpara><title>Parameters</title> <variablelist> <varlistentry> <term><replaceable class="parameter">midifile</replaceable></term>
 * <listitem><para>the filename of the MIDI file that should be displayed</para></listitem> </varlistentry> </variablelist>
 * </formalpara>
 *
 * <formalpara><title>Bugs, limitations</title> <para>Meta and system common events are not displayed in detail.</para>
 * </formalpara>
 *
 * <formalpara><title>Source code</title> <para> <ulink url="DumpSequence.java.html">DumpSequence.java</ulink>, <ulink
 * url="DumpReceiver.java.html">DumpReceiver.java</ulink> </para> </formalpara>
 *
 * -DocBookXML
 */
public class DumpSequence {
    private static String[] sm_astrKeyNames = {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    private static Receiver sm_receiver = new DumpReceiver(System.out, true);

    public static void main(String[] args) {
        /*
         * We check that there is exactely one command-line argument. If not, we display the usage message and exit.
         */
        if (args.length != 1) {
            System.out.println("DumpSequence: usage:");
            System.out.println("\tjava DumpSequence <midifile>");
            System.exit(1);
        }

        /*
         * Now, that we're shure there is an argument, we take it as the filename of the soundfile we want to play.
         */
        String strFilename = args[0];
        File midiFile = new File(strFilename);

        /*
         * We try to get a Sequence object, which the content of the MIDI file.
         */
        Sequence sequence = null;
        try {
            sequence = MidiSystem.getSequence(midiFile);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        /*
         * And now, we output the data.
         */
        if (sequence == null) {
            System.out.println("Cannot retrieve Sequence.");
        } else {
            System.out.println("---------------------------------------------------------------------------");
            System.out.println("File: " + strFilename);
            System.out.println("---------------------------------------------------------------------------");
            System.out.println("Length: " + sequence.getTickLength() + " ticks");
            System.out.println("Duration: " + sequence.getMicrosecondLength() + " microseconds");
            System.out.println("---------------------------------------------------------------------------");

            float fDivisionType = sequence.getDivisionType();
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
            if (sequence.getDivisionType() == Sequence.PPQ) {
                strResolutionType = " ticks per beat";
            } else {
                strResolutionType = " ticks per frame";
            }
            System.out.println("Resolution: " + sequence.getResolution() + strResolutionType);
            System.out.println("---------------------------------------------------------------------------");

            Track[] tracks = sequence.getTracks();
            for (int nTrack = 0; nTrack < tracks.length; nTrack++) {
                System.out.println("Track " + nTrack + ":");
                System.out.println("-----------------------");

                Track track = tracks[nTrack];
                for (int nEvent = 0; nEvent < track.size(); nEvent++) {
                    MidiEvent event = track.get(nEvent);
                    output(event);
                }
                System.out.println("---------------------------------------------------------------------------");
            }

            // TODO: getPatchList()
        }
    }

    public static void output(MidiEvent event) {
        MidiMessage message = event.getMessage();
        long lTicks = event.getTick();
        sm_receiver.send(message, lTicks);
    }
}
