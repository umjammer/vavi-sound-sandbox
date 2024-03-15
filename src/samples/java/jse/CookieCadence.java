package jse;
/*
 *        CookieCadence.java
 *
 *        This file is part of the Java Sound Examples.
 */
/*
 *  Copyright (c) 2000 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
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
import java.io.File;
import java.io.IOException;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;


// IDEA: if filename is omitted, play it instantly.
public class CookieCadence {
    /*
     *        This velocity is used for all notes.
     */
    private static final int VELOCITY = 127;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("usage:");
            System.out.println("java CookieCadence <midifile>");
            System.exit(1);
        }

        String strFilename = args[0];
        Sequence sequence = null;
        try {
            sequence = new Sequence(Sequence.PPQ, 500000);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
            System.exit(1);
        }

        Track track = sequence.createTrack();

        // first chord: C major
        track.add(createNoteOnEvent(61, 0));
        track.add(createNoteOffEvent(61, 10000));

        // second chord: f minor N
        // third chord: C major 6-4
        // forth chord: G major7
        // fifth chord: C major
        try {
            MidiSystem.write(sequence, 0, new File(strFilename));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        /*
         *        This is only necessary because of a bug in the Sun jdk1.3/1.4
         */
        System.exit(0);
    }

    private static MidiEvent createNoteOnEvent(int nKey, long lTick) {
        return createNoteEvent(ShortMessage.NOTE_ON, nKey, VELOCITY, lTick);
    }

    private static MidiEvent createNoteOffEvent(int nKey, long lTick) {
        return createNoteEvent(ShortMessage.NOTE_OFF, nKey, 0, lTick);
    }

    private static MidiEvent createNoteEvent(int nCommand, int nKey,
                                             int nVelocity, long lTick) {
        ShortMessage message = new ShortMessage();
        try {
            message.setMessage(nCommand, 0, // always on channel 1
                               nKey, nVelocity);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
            System.exit(1);
        }

        MidiEvent event = new MidiEvent(message, lTick);
        return event;
    }
}
