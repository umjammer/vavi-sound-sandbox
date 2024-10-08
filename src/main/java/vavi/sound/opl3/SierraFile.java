/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.opl3;

import java.io.DataInputStream;
import java.io.IOException;

import vavi.sound.midi.opl3.Opl3Soundbank.Opl3Instrument;
import vavi.sound.midi.opl3.Opl3Synthesizer.Context;
import vavi.sound.opl3.MidPlayer.MidiTypeFile;


/**
 * SierraFile (SCI).
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/25 umjammer initial version <br>
 */
class SierraFile extends MidiTypeFile {

    @Override
    int markSize() {
        return 3;
    }

    @Override
    boolean matchFormatImpl(DataInputStream dis) throws IOException {
        return dis.readUnsignedByte() == 0x84 &&
                dis.readUnsignedByte() == 0 &&
                dis.readUnsignedByte() != 0xf0;
    }

    protected final Opl3Instrument[] smyinsbank = new Opl3Instrument[128];
    // sierra instruments
    protected int stins;

    private final boolean[] ons = new boolean[16];
    private final int[] inums = new int[16];

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

    protected Context context;

    @Override
    public void init(Context context) {
        this.context = context;

        if (smyinsbank[0] != null) {
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

    @Override
    public int nativeVelocity(int channel, int velocity) {
//        if ((adlib.style & Adlib.MIDI_STYLE) != 0) {
        int nv = (context.voiceStatus()[channel].volume * velocity) / 128;

        if (nv > 127) {
            nv = 127;
        }

        nv = Adlib.my_midi_fm_vol_table[nv];
        return nv;
//        }
    }
}
