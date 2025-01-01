/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package jp.or.rim.kt.kemusiro.sound;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;


/**
 * MidiConvertible.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-12-07 nsano initial version <br>
 */
public interface MidiConvertible {

    /** */
    MidiEvent[] convert(MidiContext context) throws InvalidMidiDataException;

    /** */
    class MidiContext {

        /** */
        public int getMidiTempo(int tempo) {
            return tempo * 200;
        }

        /** */
        public int getProgram(Instrument instrument) {
            return switch (instrument) {
                case SquareWaveInstrument ignore -> 0;
                case SineWaveInstrument ignore -> 1;
                case FMGeneralInstrument ignore -> 2;
                default -> throw new IllegalArgumentException("Unexpected value: " + instrument);
            };
        }

        /** */
        public int getBank(Instrument instrument) {
            return switch (instrument) {
                case SquareWaveInstrument ignore -> 0;
                case SineWaveInstrument ignore -> 0;
                case FMGeneralInstrument fmGeneralInstrument -> 0 /* fmGeneralInstrument.*/; // TODO
                default -> throw new IllegalArgumentException("Unexpected value: " + instrument);
            };
        }
    }
}
