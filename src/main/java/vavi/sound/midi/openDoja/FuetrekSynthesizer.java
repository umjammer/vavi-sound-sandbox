/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.openDoja;

import javax.sound.midi.MidiDevice;
import opendoja.audio.mld.SamplerProvider;
import opendoja.audio.mld.fuetrek.FueTrekSamplerProvider;

import static vavi.sound.midi.openDoja.FuetrekMidiDeviceProvider.version;


/**
 * FuetrekSynthesizer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/07/09 nsano initial version <br>
 */
public class FuetrekSynthesizer extends OpenDojaSynthesizer {

    protected static final MidiDevice.Info info =
        new MidiDevice.Info("Fuetrek MIDI Synthesizer",
                            "vavi",
                            "Software synthesizer using openDoja Fuetrek",
                            "Version " + version) {};

    @Override
    public Info getDeviceInfo() {
        return info;
    }

    @Override
    protected SamplerProvider createSamplerProvider() {
        return new FueTrekSamplerProvider();
    }
}
