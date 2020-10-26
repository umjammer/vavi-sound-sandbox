/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.uva.emulation;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.logging.Logger;

import org.uva.emulation.MidPlayer.MidiTypeFile;
import org.uva.emulation.Opl3SoundBank.Opl3Instrument;

import vavi.util.StringUtil;


/**
 * CmfFile.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/25 umjammer initial version <br>
 */
class CmfFile extends MidiTypeFile {
    static Logger logger = Logger.getLogger(CmfFile.class.getName());

    int markSize() {
        return 4;
    }

    boolean matchFormatImpl(DataInputStream dis) throws IOException {
        return dis.readUnsignedByte() == 'C' &&
                dis.readUnsignedByte() == 'T' &&
                dis.readUnsignedByte() == 'M' &&
                dis.readUnsignedByte() == 'F';
    }

    int tins;
    Opl3Instrument[] instruments;

    @Override
    void rewind(int subSong, MidPlayer player) throws IOException {
logger.fine("\n" + StringUtil.getDump(player.data, 0, 256));
        player.takeBE(4); // ctmf

        int v = player.takeLE(2); // version

        int n = player.takeLE(2); // instrument offset
        int m = player.takeLE(2); // music offset
        player.deltas = player.takeLE(2);  //ticks/qtr note
        //the stuff in the cmf is click ticks per second...
        player.msqtr = 1000000 / player.takeLE(2) * player.deltas;
        player.takeLE(2 * 3);

        player.takeBE(16); // channel in use table...

        if (v == 0x0100) {
            player.tins = player.takeBE(1); // num instr
        } else { // 0x0101
            player.tins = player.takeLE(2); // num instr
            if (player.tins > 128) { // to ward of bad numbers...
                player.tins = 128;
            }
            player.takeLE(2); //basic tempo
        }
logger.info(String.format("numinstr: 0x%04x", player.tins));
        this.tins = player.tins;
        logger.info(String.format("ioff: %d, moff: %d, deltas: %d, msqtr: %d, numi: %d", n, m, player.deltas, player.msqtr, player.tins));

//        title = new String(data, v, ByteUtil.strlen(data, v));
//        author = new String(data, v, ByteUtil.strlen(data, v));
//        remarks = new String(data, v, ByteUtil.strlen(data, v));

        player.takeBE(n - 40);
logger.info(String.format("pos1: 0x%04x", player.pos));

        this.instruments = new Opl3Instrument[this.tins];
        for (int p = 0; p < player.tins; ++p) {
            logger.fine(String.format("\n%d: ", p));

            int[] x = new int[16];
            for (int j = 0; j < 16; ++j) {
                x[j] = player.takeBE(1);
            }
            this.instruments[p] =  Opl3SoundBank.newInstrument(0, p, "oldlucas." + p, x);
        }
logger.info(String.format("pos2: 0x%04x", player.pos));

        player.tracks[0].on = true;
        player.tracks[0].tend = player.flen; // music until the end of the file
        player.tracks[0].spos = m; // jump to midi music
    }

    @Override
    void init(Opl3Synthesizer synthesizer) {
        for (int p = 0; p < this.tins; ++p) {
            synthesizer.instruments[p] = this.instruments[p];
        }

        for (int c = 0; c < 16; ++c) {
            synthesizer.channels[c].nshift = -13;
        }

        synthesizer.adlib.style = Adlib.CMF_STYLE;
    }
}

/* */
