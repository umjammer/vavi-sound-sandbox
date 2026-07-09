/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.karplusStrong;

import javax.sound.midi.Instrument;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.midi.SoundbankResource;

import vavi.sound.karplusStrong.GuitarString;


/**
 * KarplusStrongSoundbank.
 * <p>
 * the karplus strong algorithm simulates a plucked string only,
 * so every program is mapped to the same single instrument.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2026/07/04 umjammer initial version <br>
 */
public class KarplusStrongSoundbank implements Soundbank {

    /** */
    private final Instrument[] instruments;

    /** */
    public KarplusStrongSoundbank() {
        instruments = new Instrument[] {
            new KarplusStrongInstrument(this, 0, 0, "Plucked String")
        };
    }

    @Override
    public String getName() {
        return "KarplusStrongSoundbank";
    }

    @Override
    public String getVersion() {
        return KarplusStrongSynthesizer.info.getVersion();
    }

    @Override
    public String getVendor() {
        return KarplusStrongSynthesizer.info.getVendor();
    }

    @Override
    public String getDescription() {
        return "Soundbank for the karplus strong algorithm";
    }

    @Override
    public SoundbankResource[] getResources() {
        return getInstruments();
    }

    @Override
    public Instrument[] getInstruments() {
        return instruments;
    }

    @Override
    public Instrument getInstrument(Patch patch) {
        // single instrument, any patch maps to the plucked string
        return instruments[0];
    }

    /** */
    public static class KarplusStrongInstrument extends Instrument {

        protected KarplusStrongInstrument(KarplusStrongSoundbank soundbank, int bank, int program, String name) {
            super(soundbank, new Patch(bank, program), name, GuitarString.class);
        }

        @Override
        public Object getData() {
            return GuitarString.class;
        }
    }
}
