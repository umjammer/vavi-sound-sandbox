/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.jsyn;

import java.io.IOException;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.Patch;
import javax.sound.midi.VoiceStatus;

import com.jsyn.JSyn;
import com.jsyn.unitgen.LineOut;
import com.jsyn.unitgen.UnitVoice;
import com.jsyn.util.VoiceDescription;
import com.softsynth.math.AudioMath;
import com.softsynth.shared.time.TimeStamp;
import com.sun.media.sound.ModelAbstractOscillator;
import com.sun.media.sound.ModelPatch;
import com.sun.media.sound.SimpleInstrument;

import vavi.util.Debug;


/**
 * JSynOscillator.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/11/06 umjammer initial version <br>
 */
@SuppressWarnings("restriction")
public class JSynOscillator extends ModelAbstractOscillator {

    /** */
    public class JSynInstrument extends SimpleInstrument {
        UnitVoice data;
        protected JSynInstrument(int bank, int program, boolean isPercussion, String name, UnitVoice data) {
            setName(JSynOscillator.this.getName());
            add(getPerformer());
            setPatch(new ModelPatch(bank, program, isPercussion));
            this.data = data;
        }

        @Override
        public Class<?> getDataClass() {
            return VoiceDescriptionAdapter.class;
        }

        @Override
        public Object getData() {
            return data;
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

    /** */
    private final Instrument[] instruments;

    {
        instruments = new Instrument[2];
        instruments[0] = new JSynInstrument(0, 0, false, "instrument.0.0", new com.jsyn.instruments.DualOscillatorSynthVoice());
        instruments[1] = new JSynInstrument(0, 2, true, "instrument.0.2", new com.jsyn.instruments.DrumWoodFM());
//        instruments[2] = new JSynInstrument(0, 1, true, "instrument.0.1", new com.jsyn.instruments.NoiseHit());
//        instruments[3] = new JSynInstrument(0, 3, true, "instrument.0.3", new com.jsyn.unitgen.RedNoise());
    }

    @Override
    public void init() {
        synth = JSyn.createSynthesizer();
        synth.add(lineOut = new LineOut());
        synth.start();
        lineOut.start();
        super.init();
    }

    private static float sampleRate = -1;

    @Override
    public void setSampleRate(float sampleRate) {
        if (JSynOscillator.sampleRate != sampleRate) {
Debug.println("sampleRate: " + sampleRate);
            JSynOscillator.sampleRate = sampleRate;
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

    private UnitVoice unitVoice;
    private com.jsyn.Synthesizer synth;
    private LineOut lineOut;

    private static final int BUFFER_SIZE = 64;
    private final float[] extraBuf = new float[BUFFER_SIZE];
    private int extraBufSize;

    @Override
    public void noteOn(MidiChannel channel, VoiceStatus voice, int noteNumber, int velocity) {
        if (velocity > 0) {
            unitVoice = (UnitVoice) getInstrument(new Patch(voice.bank, voice.program)).getData();
            double amplitude = velocity * (1.0 / 127);
            double frequency = AudioMath.pitchToFrequency(noteNumber);
            unitVoice.noteOn(frequency, amplitude, new TimeStamp(0));
            super.noteOn(channel, voice, noteNumber, velocity);
        } else {
            unitVoice.noteOff(new TimeStamp(0));
            super.noteOff(velocity);
        }
    }

    @Override
    public void noteOff(int velocity) {
        unitVoice.noteOff(new TimeStamp(0));
        super.noteOff(velocity);
    }

    @Override
    public int read(float[][] buffers, int offset, int len) throws IOException {

        // Grab channel 0 buffer from buffers
        float[] buffer = buffers[0];

        int i = 0;
        for (; i < len && i < extraBufSize; i++) {
            buffer[offset + i] = extraBuf[i];
        }
        if (extraBufSize > len) {
            for (int j = 0; j < extraBufSize - len; j++) {
                extraBuf[j] = extraBuf[j + len];
            }
            extraBufSize -= len;
            return len;
        }

        lineOut.generate(offset, offset + len);
        double[] values = lineOut.getSynthesisEngine().getInputBuffer(0);
Debug.println("@@@: " + values.length + ", " + len);
        for (; i < offset + len; i += BUFFER_SIZE) {
            buffer[offset + i] = (float) values[i];
        }
        int jmax = len - i;
        for (int j = 0; j < BUFFER_SIZE; j++) {
            if (j < jmax) {
                buffer[offset + i + j] = (float) values[j];
            } else {
                extraBuf[j - jmax] = (float) values[j];
            }
        }

        extraBufSize = i - len;

        return len;
    }
}
