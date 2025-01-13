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
import java.util.Arrays;

import vavi.sound.midi.opl3.Opl3Soundbank;
import vavi.sound.midi.opl3.Opl3Soundbank.Opl3Instrument;
import vavi.sound.midi.opl3.Opl3Synthesizer.Context;
import vavi.sound.opl3.MidPlayer.MidiTypeFile;

import static java.lang.System.getLogger;


/**
 * CmfFile (CMF).
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/25 umjammer initial version <br>
 */
class CmfFile extends MidiTypeFile {

    private static final Logger logger = getLogger(CmfFile.class.getName());

    @Override
    int markSize() {
        return 4;
    }

    @Override
    boolean matchFormatImpl(DataInputStream dis) throws IOException {
        return dis.readUnsignedByte() == 'C' &&
                dis.readUnsignedByte() == 'T' &&
                dis.readUnsignedByte() == 'M' &&
                dis.readUnsignedByte() == 'F';
    }

    private int tins;
    private Opl3Instrument[] instruments;

    @Override
    void rewind(int subSong, MidPlayer player) throws IOException {
        player.takeBE(4); // ctmf

        int v = player.takeLE(2); // version

        int n = player.takeLE(2); // instrument offset
        int m = player.takeLE(2); // music offset
        player.deltas = player.takeLE(2);  //ticks/qtr note
        //the stuff in the cmf is click ticks per second...
        player.msqtr = 1000000 / player.takeLE(2) * player.deltas;
        int i = player.takeLE(2);
//        String title = new String(player.data, i, ByteUtil.strlen(data, i));
        i = player.takeLE(2);
//        String author = new String(data, i, ByteUtil.strlen(data, i));
        i = player.takeLE(2);
//        String remarks = new String(data, i, ByteUtil.strlen(data, i));

        player.takeBE(16); // channel in use table...

        if (v == 0x0100) {
            player.tins = player.takeBE(1); // num instr
        } else { // 0x0101
            player.tins = player.takeLE(2); // num instr
            if (player.tins > 128) { // to ward of bad numbers...
                player.tins = 128;
            }
            player.takeLE(2); // basic tempo
        }
        logger.log(Level.INFO, String.format("numinstr: 0x%04x", player.tins));
        this.tins = player.tins;
        logger.log(Level.INFO, String.format("ioff: 0x%04x, moff: 0x%04x, deltas: %d, msqtr: %d, numi: %d, v: %04x", n, m, player.deltas, player.msqtr, player.tins, v));

        player.takeBE(n - 40);
        logger.log(Level.INFO, String.format("pos1: 0x%04x", player.pos));

        this.instruments = new Opl3Instrument[this.tins];
        for (int p = 0; p < player.tins; ++p) {

            int[] x = new int[16];
            for (int j = 0; j < 16; ++j) {
                x[j] = player.takeBE(1);
            }
            this.instruments[p] = Opl3Soundbank.newInstrument(0, p, "oldlucas." + p, x);
            logger.log(Level.DEBUG, "%d: %s".formatted(p, Arrays.toString((int[]) this.instruments[p].getData())));
        }
        logger.log(Level.INFO, "pos2: 0x%04x".formatted(player.pos));

        player.tracks[0].on = true;
        player.tracks[0].tend = player.flen; // music until the end of the file
        player.tracks[0].spos = m; // jump to midi music
    }

    protected Context context;

    @Override
    public void init(Context context) {
        this.context = context;

        for (int p = 0; p < this.tins; ++p) {
            context.instruments()[p] = this.instruments[p];
        }

        for (int c = 0; c < 16; ++c) {
            context.channels()[c].nshift = -13;
        }

        context.adlib().style = Adlib.CMF_STYLE;
    }

    @Override
    public int nativeVelocity(int channel, int velocity) {
//        if ((adlib.style & Adlib.CMF_STYLE) != 0) {
        // CMF doesn't support note velocity (even though some files have them!)
        return 127;
//        }
    }

    @Override
    public void controlChange(int channel, int controller, int value) {
        Adlib adlib = context.adlib();
        switch (controller) {
            case 0x63 -> {
//                if ((adlib.style & Adlib.CMF_STYLE) != 0) {
                // Custom extension to allow CMF files to switch the
                // AM+VIB depth on and off (officially this is on,
                // and there's no way to switch it off.) Controller
                // values:
                // 0 == AM+VIB off
                // 1 == VIB on
                // 2 == AM on
                // 3 == AM+VIB on
                adlib.write(0xbd, (adlib.read(0xbd) & ~0xc0) | (value << 6));
//                }
            }
            case 0x67 -> { // 103: undefined
                logger.log(Level.DEBUG, "control change[%d]: (%02x): %d".formatted(channel, controller, value));
//                if ((adlib.style & Adlib.CMF_STYLE) != 0) {
                adlib.mode = value;
                if (adlib.mode == Adlib.RYTHM) {
                    adlib.write(0xbd, adlib.read(0xbd) | (1 << 5));
                } else {
                    adlib.write(0xbd, adlib.read(0xbd) & ~(1 << 5));
                }
//                }
            }
        }
    }
}
