/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.openDoja;

import javax.sound.midi.MidiDevice;
import opendoja.audio.mld.SamplerProvider;
import opendoja.audio.mld.ma3.MA3SamplerProvider;

import static vavi.sound.midi.openDoja.Ma3MidiDeviceProvider.version;


/**
 * Ma3Synthesizer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/07/09 nsano initial version <br>
 */
public class Ma3Synthesizer extends OpenDojaSynthesizer {

    protected static final MidiDevice.Info info =
        new MidiDevice.Info("Ma3 MIDI Synthesizer",
                            "vavi",
                            "Software synthesizer using openDoja MA3",
                            "Version " + version) {};

    @Override
    public Info getDeviceInfo() {
        return info;
    }

    @Override
    protected SamplerProvider createSamplerProvider() {
        return new MA3SamplerProvider(
                MA3SamplerProvider.FM_MA3_4OP,
                MA3SamplerProvider.FM_MA3_4OP,
                MA3SamplerProvider.WAVE_DRUM_MA3);
    }
}
