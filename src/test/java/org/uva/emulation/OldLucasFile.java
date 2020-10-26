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


/**
 * OldLucasFile.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/25 umjammer initial version <br>
 */
class OldLucasFile extends MidiTypeFile {

    static Logger logger = Logger.getLogger(OldLucasFile.class.getName());

    int markSize() {
        return 6;
    }

    boolean matchFormatImpl(DataInputStream dis) throws IOException {
        dis.skipBytes(4);
        return dis.readUnsignedByte() == 'A' &&
                dis.readUnsignedByte() == 'D';
    }

    int tins;
    Opl3Instrument[] instruments;

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
            logger.fine(String.format("\n%d: ", p));

            int[] ins = new int[16];

            for (int i = 0; i < 16; ++i) {
                ins[i] = player.takeBE(1);
            }

            int[] x = Opl3SoundBank.fromOldLucas(ins);
            this.instruments[p] = Opl3SoundBank.newInstrument(0, p, "oldlucas." + p, x);
        }

        player.tracks[0].on = true;
        player.tracks[0].tend = player.flen; // music until the end of the file
        player.tracks[0].spos = 0x98; // jump to midi music
    }

    @Override
    void init(Opl3Synthesizer synthesizer) {
        for (int p = 0; p < this.tins; ++p) {
            synthesizer.instruments[p] = this.instruments[p];
        }

        for (int c = 0; c < 16; ++c) {
            if (c < this.tins) {
                synthesizer.channels[c].inum = c;

                synthesizer.channels[c].setIns(synthesizer.instruments[synthesizer.channels[c].inum]);
            }
        }

        synthesizer.adlib.style = Adlib.LUCAS_STYLE | Adlib.MIDI_STYLE;
    }
}

/* */
