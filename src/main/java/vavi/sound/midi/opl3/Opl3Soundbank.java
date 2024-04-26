/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.opl3;

import javax.sound.midi.Instrument;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.midi.SoundbankResource;


/**
 * Opl3Soundbank.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/23 umjammer initial version <br>
 */
public class Opl3Soundbank implements Soundbank {

    /** */
    public Opl3Soundbank(int[][] defaultInstruments) {
        instruments = new Instrument[defaultInstruments.length];
        for (int i = 0; i < instruments.length; i++) {
            int[] b = new int[16];
            System.arraycopy(defaultInstruments[i], 0, b, 0, defaultInstruments[i].length);
            b[14] = 0;
            b[15] = 0;
            instruments[i] = new Opl3Instrument(this, 0, i, "instrument." + i, b);
        }
    }

    /** */
    private final Instrument[] instruments;

    @Override
    public String getName() {
        return "Opl3Soundbank";
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
        return "Soundbank for opl3";
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
    public static Opl3Instrument newInstrument(int bank, int program, String name, int[] data) {
        return new Opl3Instrument(null, bank, program, name, data);
    }

    /** */
    public static class Opl3Instrument extends Instrument {
        int[] data;
        protected Opl3Instrument(Opl3Soundbank sounBbank, int bank, int program, String name, int[] data) {
            super(sounBbank, new Patch(bank, program), name, int[].class);
            this.data = data;
        }

        @Override
        public Object getData() {
            return data;
        }
    }
}
