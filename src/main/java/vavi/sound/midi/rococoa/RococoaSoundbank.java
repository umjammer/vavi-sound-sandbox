/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.rococoa;

import javax.sound.midi.Instrument;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.midi.SoundbankResource;


/**
 * RococoaSoundbank.
 * <p>
 * for AVAudioUnitSampler
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/30 umjammer initial version <br>
 */
public class RococoaSoundbank implements Soundbank {

    /** */
    public RococoaSoundbank() {
    }

    /** */
    private Instrument[] instruments;

    @Override
    public String getName() {
        return "RococoaSoundbank";
    }

    @Override
    public String getVersion() {
        return RococoaSynthesizer.info.getVersion();
    }

    @Override
    public String getVendor() {
        return RococoaSynthesizer.info.getVendor();
    }

    @Override
    public String getDescription() {
        return "Soundbank for AVFoundation";
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
    public static class RococoaInstrument extends Instrument {
        final Object data;
        protected RococoaInstrument(RococoaSoundbank sounBbank, int bank, int program, String name, Object data) {
            super(sounBbank, new Patch(bank, program), name, Object.class);
            this.data = data;
        }

        @Override
        public Object getData() {
            return data;
        }
    }
}
