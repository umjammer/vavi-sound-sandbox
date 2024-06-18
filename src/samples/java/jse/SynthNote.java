/*
 *  Copyright (c) 1999 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

import static java.lang.System.getLogger;


/*        +DocBookXML
        <title>Playing a note on the synthesizer</title>

        <formalpara><title>Purpose</title>
        <para>Plays a single note on the synthesizer.</para>
        </formalpara>

        <formalpara><title>Level</title>
        <para>newbie</para></formalpara>

        <formalpara><title>Usage</title>
        <para>
        <cmdsynopsis><command>java SynthNote</command>
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
        <para>The precision of the duration depends on the precision
        of <function>Thread.sleep()</function>, which in turn depends on
        the precision of the system time and the latency of th
        thread scheduling of the Java VM. For many VMs, this
        means about 20 ms. When playing multiple notes, it is
        recommended to use a <classname>Sequence</classname> and the
        <classname>Sequencer</classname>, which is supposed to give better
        timing.</para>
        </formalpara>

        <formalpara><title>Source code</title>
        <para>
        <ulink url="SynthNote.java.html">SynthNote.java</ulink>
        </para>
        </formalpara>

-DocBookXML
*/

/**
 * SynthNote.java
 * <p>
 * This file is part of the Java Sound Examples.
 */
public class SynthNote {

    private static final Logger logger = getLogger(SynthNote.class.getName());

    public static void main(String[] args) {
        int nNoteNumber = 0; // MIDI key number
        int nVelocity = 0;

        // Time between note on and note off event in
        // milliseconds. Note that on most systems, the
        // best resolution you can expect are 10 ms.
        int nDuration = 0;
        if (args.length == 3) {
            nNoteNumber = Integer.parseInt(args[0]);
            nNoteNumber = Math.min(127, Math.max(0, nNoteNumber));
            nVelocity = Integer.parseInt(args[1]);
            nVelocity = Math.min(127, Math.max(0, nVelocity));
            nDuration = Integer.parseInt(args[2]);
            nDuration = Math.max(0, nDuration);
        } else {
            System.out.println("SynthNote: usage:");
            System.out.println("java SynthNote <note number> <velocity> <duration>");
            System.exit(1);
        }

        // We need a synthesizer to play the note on.
        // Here, we simply request the default
        // synthesizer.
        Synthesizer synth = null;
        try {
            synth = MidiSystem.getSynthesizer();
        } catch (MidiUnavailableException e) {
        }

        // Of course, we have to open the synthesizer to
        // produce any sound for us.
        try {
            synth.open();
        } catch (MidiUnavailableException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            System.exit(1);
        }

        // Turn the note on on MIDI channel 1.
        // (Index zero means MIDI channel 1)
        MidiChannel[] channels = synth.getChannels();
        channels[0].noteOn(nNoteNumber, nVelocity);

        // Wait for the specified amount of time
        // (the duration of the note).
        try {
            Thread.sleep(nDuration);
        } catch (InterruptedException e) {
        }

        // Turn the note off.
        channels[0].noteOff(nNoteNumber);
    }
}
