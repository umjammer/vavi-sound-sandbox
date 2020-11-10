/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mocha;

import java.io.IOException;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.Patch;
import javax.sound.midi.VoiceStatus;

import com.sun.media.sound.ModelAbstractOscillator;
import com.sun.media.sound.ModelPatch;
import com.sun.media.sound.SimpleInstrument;

import vavi.util.Debug;

import mocha.sound.Instrumental;


/**
 * MochaOscillator.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/11/06 umjammer initial version <br>
 */
@SuppressWarnings("restriction")
public class MochaOscillator extends ModelAbstractOscillator {

    /** */
    public class MochaInstrument extends SimpleInstrument {
        Instrumental data;
        protected MochaInstrument(int bank, int program, boolean isPercussion, String name, Instrumental data) {
            setName(MochaOscillator.this.getName());
            add(getPerformer());
            setPatch(new ModelPatch(bank, program, isPercussion));
            this.data = data;
        }

        @Override
        public Class<?> getDataClass() {
            return Instrumental.class;
        }

        @Override
        public Object getData() {
            return data;
        }
    }

    /** */
    Instrument[] instruments;

    {
        instruments = new Instrument[4];
        instruments[0] = new MochaInstrument(0, 0, false, "instrument.0.0", new mocha.sound.soundbank.FMStrings());
        instruments[1] = new MochaInstrument(0, 1, false, "instrument.0.1", new mocha.sound.soundbank.FirstInstrument());
        instruments[2] = new MochaInstrument(0, 2, false, "instrument.0.2", new mocha.sound.soundbank.SecondInstrument());
    }

    @Override
    public void init() {
        super.init();
    }

    static float sampleRate = -1;

    @Override
    public void setSampleRate(float sampleRate) {
        if (MochaOscillator.sampleRate != sampleRate) {
Debug.println("sampleRate: " + sampleRate);
            MochaOscillator.sampleRate = sampleRate;
        }
        super.setSampleRate(sampleRate);
    }

    @Override
    public Instrument[] getInstruments() {
        for (Instrument i : instruments) {
            ((SimpleInstrument) i).add(getPerformer());
        }
        return instruments;
    }

    @Override
    public Instrument getInstrument(Patch patch) {
//Debug.println("patch: " + patch.getBank() + "," + patch.getProgram());
        for (Instrument ins : instruments) {
            Patch p = ins.getPatch();
            if (p.getBank() != patch.getBank())
                continue;
            if (p.getProgram() != patch.getProgram())
                continue;
            if (p instanceof ModelPatch && patch instanceof ModelPatch) {
                if (((ModelPatch)p).isPercussion()
                        != ((ModelPatch)patch).isPercussion()) {
                    continue;
                }
            }
//Debug.println("instrument: " + ins);
            return ins;
        }
Debug.println("instrument not found for: " + patch.getBank() + "," + patch.getProgram());
        return instruments[0];
    }

    @Override
    public void noteOn(MidiChannel channel, VoiceStatus voice, int noteNumber, int velocity) {
        if (velocity > 0) {
            super.noteOn(channel, voice, noteNumber, velocity);
        } else {
            super.noteOff(velocity);
        }
    }

    @Override
    public int read(float[][] buffers, int offset, int len) throws IOException {

        // Grab channel 0 buffer from buffers
        float[] buffer = buffers[0];

        

        return len;
    }
}
