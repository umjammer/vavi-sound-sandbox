/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.uva.emulation;

import java.io.DataInputStream;
import java.io.IOException;

import org.uva.emulation.MidPlayer.MidiTypeFile;
import org.uva.emulation.Opl3SoundBank.Opl3Instrument;


/**
 * SierraFile.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/25 umjammer initial version <br>
 */
class SierraFile extends MidiTypeFile {

    int markSize() {
        return 3;
    }

    boolean matchFormatImpl(DataInputStream dis) throws IOException {
        return dis.readUnsignedByte() == 0x84 &&
                dis.readUnsignedByte() == 0 &&
                dis.readUnsignedByte() != 0xf0;
    }

    Opl3Instrument[] smyinsbank = new Opl3Instrument[128];
    // sierra instruments
    int stins;

    boolean[] ons = new boolean[16];
    int[] inums = new int[16];

    @Override
    void rewind(int subSong, MidPlayer player) throws IOException {
        player.tins = stins;
        player.takeBE(3);
        player.deltas = 32;
        player.tracks[0].on = true;
        player.tracks[0].tend = player.flen; // music until the end of the file

        for (int c = 0; c < 16; ++c) {
            this.ons[c] = player.takeBE(1) != 0;
            this.inums[c] = player.takeBE(1);
        }

        player.tracks[0].spos = player.pos;
    }

    @Override
    void init(Opl3Synthesizer synthesizer) {
        if (smyinsbank != null) {
            synthesizer.instruments = smyinsbank;
        }

        for (int c = 0; c < 16; ++c) {
            synthesizer.channels[c].nshift = -13;
            synthesizer.voiceStatus[c].active = this.ons[c];
            synthesizer.channels[c].inum = this.inums[c];

            synthesizer.channels[c].setIns(synthesizer.instruments[synthesizer.channels[c].inum]);
        }

        synthesizer.adlib.style = Adlib.SIERRA_STYLE | Adlib.MIDI_STYLE;
    }
}

/* */
