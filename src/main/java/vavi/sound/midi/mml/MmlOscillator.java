/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mml;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.Patch;
import javax.sound.midi.VoiceStatus;

import com.sun.media.sound.ModelAbstractOscillator;
import com.sun.media.sound.SimpleInstrument;

import static java.lang.System.getLogger;


/**
 * MmlOscillator.
 * <p>
 * how to implement an oscillator.
 * <ul>
 *  <li>make it stateless except sampling rate (oscillator instances are created multiply at playing)</li>
 *  <li>a sound engine should be separated from the oscillator, hold active notes and be referenced as a singleton</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 241206 nsano initial version <br>
 */
@SuppressWarnings("restriction")
public class MmlOscillator extends ModelAbstractOscillator {

    private static final Logger logger = getLogger(MmlOscillator.class.getName());

    private static final MmlSoundbank soundbank;

    static {
        soundbank = new MmlSoundbank();
    }

    private static class ActiveNote {
        int number;
        int velocity;
        jp.or.rim.kt.kemusiro.sound.Instrument inst;
        public void setActive(int number, int velocity) {
            this.number = number;
            this.velocity = velocity;
        }
    }

    private int samplingRate;
    private int currentTempo = 60;
    // TODO should be thread local?
    private static final Map<Integer, Supplier<jp.or.rim.kt.kemusiro.sound.Instrument>> channelInsts = new HashMap<>();
    // TODO should be thread local?
    private static final Map<String, ActiveNote> activeNotes = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static jp.or.rim.kt.kemusiro.sound.Instrument getInst(VoiceStatus voice) {
        Supplier<jp.or.rim.kt.kemusiro.sound.Instrument> supplier = channelInsts.get(voice.channel);
        if (supplier == null) {
            return ((Supplier<jp.or.rim.kt.kemusiro.sound.Instrument>) soundbank.getInstrument(new Patch(voice.bank, voice.volume)).getData()).get();
        } else {
            return supplier.get();
        }
    }

    private static String key(VoiceStatus voice) {
        return voice.channel + "." + voice.note;
    }

    @Override
    public void init() {
//logger.log(Level.DEBUG, "init: @" + hashCode());
        super.init();
    }

    @Override
    public void setSampleRate(float sampleRate) {
        this.samplingRate = (int) sampleRate;
//logger.log(Level.TRACE, "samplingRate: " + samplingRate + ", @" + hashCode());
        super.setSampleRate(sampleRate);
    }

    @Override
    public Instrument[] getInstruments() {
        Instrument[] instruments = soundbank.getInstruments();
        for (Instrument i : instruments) {
            ((SimpleInstrument) i).add(getPerformer()); // important!
        }
        return instruments;
    }

    @Override
    public Instrument getInstrument(Patch patch) {
        return soundbank.getInstrument(patch);
    }

    @Override
    public void noteOn(MidiChannel channel, VoiceStatus voice, int noteNumber, int velocity) {
        if (velocity > 0) {
            ActiveNote note = activeNotes.computeIfAbsent(key(voice), k -> new ActiveNote());
            note.setActive(noteNumber, velocity);
            note.inst = getInst(voice);
//logger.log(Level.TRACE, "patch: " + voice.bank + "," + voice.program + ", @" + hashCode());
            note.inst.press();
            super.noteOn(channel, voice, noteNumber, velocity);
        } else {
            noteOff(velocity);
        }
    }

    @Override
    public void noteOff(int velocity) {
        ActiveNote note = activeNotes.get(key(voice));
        if (note != null) {
            note.inst.release();
            activeNotes.remove(key(voice));
        }
        super.noteOff(velocity);
    }

    /** */
    public void meta(int meta, byte[] data) {
logger.log(Level.TRACE, "meta: %02x".formatted(meta));
        if (meta == 0x51) {
            currentTempo = (data[0] & 0xff) * 0x1_0000 + (data[1] & 0xff) * 0x100 + (data[2] & 0xff);
        }
    }

    /** */
    @SuppressWarnings("unchecked")
    public void programChange(int channel, int data1, int data2) {
logger.log(Level.TRACE, "programChange: %d, %02x, %02x, @%d".formatted(channel, data1, data2, hashCode()));
        channelInsts.put(channel, ((Supplier<jp.or.rim.kt.kemusiro.sound.Instrument>) soundbank.getInstrument(new Patch(data1, data2)).getData()));
    }

    @Override
    public int read(float[][] buffers, int offset, int len) throws IOException {
        // Grab channel 0 buffer from buffers
        float[] buffer = buffers[0];

        ActiveNote note = activeNotes.get(key(voice));
        if (note != null)
            note.inst.setTimeStep(1.0 / (double) samplingRate);

        for (int i = 0; i < len; i++) {
            float value = 0.0f;
            if (note != null) {
                value += (float) ((note.velocity / 127f) * note.inst.getValue(note.number));
            }
            buffer[offset + i] = value;
        }

        return len;
    }
}
