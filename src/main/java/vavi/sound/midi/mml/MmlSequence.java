/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mml;

import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.LinkedList;
import java.util.Scanner;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;

import jp.or.rim.kt.kemusiro.sound.MMLCompiler;
import jp.or.rim.kt.kemusiro.sound.MMLException;
import jp.or.rim.kt.kemusiro.sound.MidiConvertible;
import jp.or.rim.kt.kemusiro.sound.MidiConvertible.MidiContext;
import jp.or.rim.kt.kemusiro.sound.MusicEvent;
import jp.or.rim.kt.kemusiro.sound.MusicScore;

import static java.lang.System.getLogger;


/**
 * MmlSequence.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 241206 nsano initial version <br>
 */
public class MmlSequence {

    private static final Logger logger = getLogger(MmlSequence.class.getName());

    private LinkedList<MusicEvent> events;
    int channels;
    int tickPerBeat = 240;

    /** */
    public void setScore(InputStream is) throws MMLException {
        // gather multiple lines into one track
        StringBuilder result = new StringBuilder();
        Scanner scanner = new Scanner(is);
        while (scanner.hasNextLine()) {
            result.append(scanner.nextLine());
        }
        String[] tracks = new String[] {result.toString()};

        channels = tracks.length;

        MusicScore score = new MusicScore(tickPerBeat, channels);
        MMLCompiler compiler = new MMLCompiler(tickPerBeat, channels);
        compiler.compile(score, tracks);
        events = score.getEventList();
    }

    /** */
    public Sequence toMidiSequence() throws InvalidMidiDataException {
        Sequence sequence = new Sequence(Sequence.PPQ, 48, channels);

        MidiContext context = new MidiContext();

        for (MusicEvent e : events) {
            int ch = e.getChannel();

            if (e instanceof MidiConvertible midiConvertible) {
                for (MidiEvent event : midiConvertible.convert(context)) {
                    sequence.getTracks()[ch].add(event);
                }
            } else {
logger.log(Level.WARNING, "unhandled event: " + e);
            }
        }

        return sequence;
    }
}
