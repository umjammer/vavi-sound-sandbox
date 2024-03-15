/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.jsyn;

import javax.sound.midi.Instrument;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.midi.SoundbankResource;

import com.jsyn.unitgen.UnitVoice;
import com.jsyn.util.VoiceDescription;


/**
 * JSynSoundbank.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/30 umjammer initial version <br>
 */
public class JSynSoundbank implements Soundbank {

    /** */
    public JSynSoundbank() {
        instruments = new Instrument[4];
        instruments[0] = new JSynInstrument(this, 0, 0, "instrument.0.0", new com.jsyn.instruments.DualOscillatorSynthVoice());
        instruments[1] = new JSynInstrument(this, 0, 1, "instrument.0.1", new com.jsyn.instruments.NoiseHit());
        instruments[2] = new JSynInstrument(this, 0, 2, "instrument.0.2", new com.jsyn.instruments.DrumWoodFM());
        instruments[3] = new JSynInstrument(this, 0, 3, "instrument.0.3", new com.jsyn.unitgen.RedNoise());
    }

    /** */
    private final Instrument[] instruments;

    @Override
    public String getName() {
        return "JSynSoundbank";
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
        return "Soundbank for JSyn";
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
    public static class JSynInstrument extends Instrument {
        UnitVoice data;
        protected JSynInstrument(JSynSoundbank sounBbank, int bank, int program, String name, UnitVoice data) {
            super(sounBbank, new Patch(bank, program), name, VoiceDescriptionAdapter.class);
            this.data = data;
        }

        @Override
        public Object getData() {
            return new VoiceDescriptionAdapter(data);
        }
    }

    static class VoiceDescriptionAdapter extends VoiceDescription {
        UnitVoice unitVoice;
        public VoiceDescriptionAdapter(UnitVoice unitVoice) {
            super(unitVoice.getClass().getSimpleName(), new String[] { unitVoice.getClass().getSimpleName() });
            this.unitVoice = unitVoice;
        }

        @Override
        public UnitVoice createUnitVoice() {
            return unitVoice;
        }

        @Override
        public String[] getTags(int presetIndex) {
            return new String[0];
        }

        @Override
        public String getVoiceClassName() {
            return unitVoice.getClass().getName();
        }
    }
}
