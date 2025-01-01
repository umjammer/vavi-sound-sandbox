/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mml;

import java.io.IOException;
import java.lang.System.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import javax.sound.midi.Instrument;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.midi.SoundbankResource;

import com.sun.media.sound.ModelPatch;
import com.sun.media.sound.SimpleInstrument;
import jp.or.rim.kt.kemusiro.sound.FMGeneralInstrument;
import jp.or.rim.kt.kemusiro.sound.SineWaveInstrument;
import jp.or.rim.kt.kemusiro.sound.SquareWaveInstrument;

import static java.lang.System.getLogger;


/**
 * MmlSoundbank.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2024/12/06 umjammer initial version <br>
 */
@SuppressWarnings("restriction")
public class MmlSoundbank implements Soundbank {

    private static final Logger logger = getLogger(MmlSoundbank.class.getName());

    /** instruments, key = bank# + "." + program# (percussion "p." + bank# + "." + program#) */
    private static final Map<String, Instrument> instruments = new HashMap<>();

    static {
        try {
            FMGeneralInstrument.readParameterByResource();
            int[] toneNumbers = FMGeneralInstrument.getToneNumbers();

            instruments.put("0.0", new MmlInstrument(0, 0, false, SquareWaveInstrument::new));
            instruments.put("1.0", new MmlInstrument(1, 0, false, SineWaveInstrument::new));
            IntStream.range(0, toneNumbers.length).forEach(i -> {
                instruments.put("2." + i, new MmlInstrument(2, i, false, () -> new FMGeneralInstrument(toneNumbers[i])));
            });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** */
    public static class MmlInstrument extends SimpleInstrument {
        Supplier<jp.or.rim.kt.kemusiro.sound.Instrument> data;
        protected MmlInstrument(int bank, int program, boolean isPercussion, Supplier<jp.or.rim.kt.kemusiro.sound.Instrument> data) {
            setPatch(new ModelPatch(bank, program, isPercussion));
            this.data = data;
        }

        @Override
        public String getName() {
            return getPatch().isPercussion() ? "Percussion" : data.getClass().getSimpleName();
        }

        @Override
        public Class<?> getDataClass() {
            return jp.or.rim.kt.kemusiro.sound.Instrument.class;
        }

        @Override
        public Object getData() {
            return data;
        }
    }

    @Override
    public String getName() {
        return "MmlSoundbank";
    }

    @Override
    public String getVersion() {
        return MmlSynthesizer.info.getVersion();
    }

    @Override
    public String getVendor() {
        return MmlSynthesizer.info.getVendor();
    }

    @Override
    public String getDescription() {
        return "MmlSoundbank";
    }

    @Override
    public SoundbankResource[] getResources() {
        return new SoundbankResource[0];
    }

    @Override
    public Instrument[] getInstruments() {
        return instruments.values().toArray(Instrument[]::new);
    }

    @Override
    public Instrument getInstrument(Patch patch) {
//logger.log(Level.DEBUG, "patch: " + patch.getBank() + "," + patch.getProgram() + ", " + patch.getClass().getName());
        Instrument ins = null;
        String k = patch.getBank() + "." + patch.getProgram();
        if (instruments.containsKey(k)) {
            ins = instruments.get(k);
        }
//logger.log(Level.TRACE, "instrument: " + ins.getPatch().getBank() + ", " + ins.getPatch().getProgram() + ", " + ins.getName());
        return ins;
    }
}
