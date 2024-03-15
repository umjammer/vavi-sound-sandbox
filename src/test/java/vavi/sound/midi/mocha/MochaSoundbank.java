/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mocha;

import javax.sound.midi.Instrument;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.midi.SoundbankResource;

import mocha.sound.Instrumental;


/**
 * MochaSoundbank.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/30 umjammer initial version <br>
 */
public class MochaSoundbank implements Soundbank {

    /** */
    public MochaSoundbank() {
        instruments = new Instrument[3];
        instruments[0] = new MochaInstrument(this, 0, 0, "instrument.0.0", new mocha.sound.soundbank.FMStrings());
        instruments[1] = new MochaInstrument(this, 0, 1, "instrument.0.1", new mocha.sound.soundbank.FirstInstrument());
        instruments[2] = new MochaInstrument(this, 0, 2, "instrument.0.2", new mocha.sound.soundbank.SecondInstrument());
    }

    /** */
    private final Instrument[] instruments;

    @Override
    public String getName() {
        return "MochaSoundbank";
    }

    @Override
    public String getVersion() {
        return "0.0.1";
    }

    @Override
    public String getVendor() {
        return "vavi";
    }

    @Override
    public String getDescription() {
        return "Soundbank for Mocha";
    }

    @Override
    public SoundbankResource[] getResources() {
        return new SoundbankResource[0];
    }

    @Override
    public Instrument[] getInstruments() {
        return instruments;
    }

    @Override
    public Instrument getInstrument(Patch patch) {
        for (Instrument instrument : instruments) {
            if (instrument.getPatch().getProgram() == patch.getProgram() &&
                    instrument.getPatch().getBank() == patch.getBank()) {
                return instrument;
            }
        }
        return null;
    }

    /** */
    public static class MochaInstrument extends Instrument {
        Instrumental data;
        protected MochaInstrument(MochaSoundbank sounBbank, int bank, int program, String name, Instrumental data) {
            super(sounBbank, new Patch(bank, program), name, Instrumental.class);
            this.data = data;
        }

        @Override
        public Object getData() {
            return data;
        }
    }
}
