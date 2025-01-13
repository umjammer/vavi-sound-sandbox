/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.opl3;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.midi.opl3.Opl3Soundbank;
import vavi.sound.midi.opl3.Opl3Soundbank.Opl3Instrument;
import vavi.sound.midi.opl3.Opl3Synthesizer.Context;
import vavi.sound.opl3.MidPlayer.MidiTypeFile;

import static java.lang.System.getLogger;


/**
 * OldLucasFile.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/25 umjammer initial version <br>
 */
class OldLucasFile extends MidiTypeFile {

    private static final Logger logger = getLogger(OldLucasFile.class.getName());

    @Override
    int markSize() {
        return 6;
    }

    @Override
    boolean matchFormatImpl(DataInputStream dis) throws IOException {
        dis.skipBytes(4);
        return dis.readUnsignedByte() == 'A' &&
                dis.readUnsignedByte() == 'D';
    }

    private int tins;
    private Opl3Instrument[] instruments;

    private static int[] fromOldLucas(int[] ins) {
        int[] x = new int[11];
        x[10] = ins[2];
        x[0] = ins[3];
        x[2] = ins[4];
        x[4] = ins[5];
        x[6] = ins[6];
        x[8] = ins[7];
        x[1] = ins[8];
        x[3] = ins[9];
        x[5] = ins[10];
        x[7] = ins[11];
        x[9] = ins[12];
        return x;
    }

    @Override
    void rewind(int subSong, MidPlayer player) throws IOException {
        player.takeBE(1);
        player.msqtr = 250000;
        player.pos = 9;
        player.deltas = player.takeBE(1);
        int v = 8;
        player.pos = 0x19; // jump to instruments
        player.tins = v;
        this.tins = player.tins;

        this.instruments = new Opl3Instrument[this.tins];
        for (int p = 0; p < v; ++p) {
logger.log(Level.DEBUG, "\n%d: ".formatted(p));

            int[] ins = new int[16];

            for (int i = 0; i < 16; ++i) {
                ins[i] = player.takeBE(1);
            }

            int[] x = fromOldLucas(ins);
            this.instruments[p] = Opl3Soundbank.newInstrument(0, p, "oldlucas." + p, x);
        }

        player.tracks[0].on = true;
        player.tracks[0].tend = player.flen; // music until the end of the file
        player.tracks[0].spos = 0x98; // jump to midi music
    }

    protected Context context;

    @Override
    public void init(Context context) {
        this.context = context;

        for (int p = 0; p < this.tins; ++p) {
            context.instruments()[p] = this.instruments[p];
        }

        for (int c = 0; c < 16; ++c) {
            if (c < this.tins) {
                context.channels()[c].inum = c;

                context.channels()[c].setIns(context.instruments()[context.channels()[c].inum]);
            }
        }

        context.adlib().style = Adlib.LUCAS_STYLE | Adlib.MIDI_STYLE;
    }

    @Override
    public int nativeVelocity(int channel, int velocity) {
//        if ((adlib.style & Adlib.MIDI_STYLE) != 0) {
        int nv = (context.voiceStatus()[channel].volume * velocity) / 128;
//        if ((adlib.style & Adlib.LUCAS_STYLE) != 0) {
        nv *= 2;
//        }

        if (nv > 127) {
            nv = 127;
        }

        nv = Adlib.my_midi_fm_vol_table[nv];
//        if ((adlib.style & Adlib.LUCAS_STYLE) != 0) {
        nv = (int) ((float) Math.sqrt((nv)) * 11.0F);
//        }
        return nv;
//        }
    }
}
