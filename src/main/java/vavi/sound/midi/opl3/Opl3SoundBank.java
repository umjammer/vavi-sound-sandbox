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
 * Opl3SoundBank.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/23 umjammer initial version <br>
 */
public class Opl3SoundBank implements Soundbank {

    /** */
    public Opl3SoundBank(int[][] defaultInstruments) {
        instruments = new Instrument[defaultInstruments.length];
        for (int i = 0; i < instruments.length; i++) {
            int[] b = new int[16];
            System.arraycopy(defaultInstruments[i], 0, b, 0, defaultInstruments[i].length);
            b[14] = 0;
            b[15] = 0;
            instruments[i] = new Opl3Instrument(this, 0, i, "ins" + i, b);
        }
    }

    /** */
    private Instrument[] instruments;

    @Override
    public String getName() {
        return "Opl3SoundBank";
    }

    @Override
    public String getVersion() {
        return "0.0.1";
    }

    @Override
    public String getVendor() {
        return "vavisoft";
    }

    @Override
    public String getDescription() {
        return "soundbank for opl3";
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
        for (int i = 0; i < instruments.length; i++) {
            if (instruments[i].getPatch().getProgram() == patch.getProgram() &&
                instruments[i].getPatch().getBank() == patch.getBank()) {
                return instruments[i];
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
        protected Opl3Instrument(Opl3SoundBank sounBbank, int bank, int program, String name, int[] data) {
            super(sounBbank, new Patch(bank, program), name, int[].class);
            this.data = data;
        }

        @Override
        public Object getData() {
            return data;
        }
    }
}

/* */
