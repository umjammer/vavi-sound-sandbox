/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.opl3;

import java.io.DataInputStream;
import java.io.IOException;

import vavi.sound.midi.opl3.Opl3SoundBank.Opl3Instrument;
import vavi.sound.midi.opl3.Opl3Synthesizer.Context;
import vavi.sound.opl3.MidPlayer.MidiTypeFile;


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
    public void init(Context context) {
        if (smyinsbank != null) {
            context.instruments(smyinsbank);
        }

        for (int c = 0; c < 16; ++c) {
            context.channels()[c].nshift = -13;
            context.voiceStatus()[c].active = this.ons[c];
            context.channels()[c].inum = this.inums[c];

            context.channels()[c].setIns(context.instruments()[context.channels()[c].inum]);
        }

        context.adlib().style = Adlib.SIERRA_STYLE | Adlib.MIDI_STYLE;
    }
}

/* */
