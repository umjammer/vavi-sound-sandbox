/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.uva.emulation;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;


/**
 * AdvancedSierraFile.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/26 umjammer initial version <br>
 */
class AdvancedSierraFile extends SierraFile {

    static Logger logger = Logger.getLogger(MidiFile.class.getName());

    int markSize() {
        return 3;
    }

    boolean matchFormatImpl(DataInputStream dis) throws IOException {
        //loadSierraIns(null)) { // TODO null
        return dis.readUnsignedByte() == 0x84 &&
                dis.readUnsignedByte() == 0 &&
                dis.readUnsignedByte() == 0xf0;
    }

    int sierra_pos;

    private boolean loadSierraIns(Path path) throws IOException {

        Path patch = path.getParent().resolve("patch.003");
        DataInputStream dis = new DataInputStream(new FileInputStream(patch.toFile()));
        dis.skipBytes(2);
        stins = 0;

        for (int j = 0; j < 2; ++j) {
            for (int k = 0; k < 48; ++k) {
                int p = j * 48 + k;
                logger.fine(String.format("%2d: ", p));

                int[] buf = new int[28];

                for (int i = 0; i < 28; ++i) {
                    buf[i] = dis.readUnsignedByte();
                }

                smyinsbank[p] = Opl3SoundBank.newInstrument(0, p, "sierra." + p, Opl3SoundBank.fromSierra(buf));

                ++stins;
            }

            dis.skipBytes(2);
        }

        dis.close();
        return true;
    }

    private void sierra_next_section(MidPlayer player) throws IOException {
        for (int t = 0; t < 16; ++t) {
            player.tracks[t].on = false;
        }

        logger.info("next adv sierra section:");
        player.pos = sierra_pos;

        int t = 0;
        for (int i = 0; i != 255; i = player.takeBE(1)) {
            player.takeBE(1);
            player.tracks[t].on = true;
            player.tracks[t].spos = player.takeBE(1);
            player.tracks[t].spos += (player.takeBE(1) << 8) + 4; // 4 best usually +3? not 0,1,2 or 5
            player.tracks[t].tend = player.flen;
            player.tracks[t].iwait = 0;
            player.tracks[t].pv = 0;
            logger.info(String.format("track %d starts at %x", t, player.tracks[t].spos));
            t++;
            player.takeBE(2);
        }

        player.takeBE(2);
        player.deltas = 32;
        sierra_pos = player.pos;
        player.fwait = 0.0F;
        player.doing = true;
    }

    @Override
    void rewind(int subSong, MidPlayer player) throws IOException {
        player.tins = stins;
        player.deltas = 32;
        player.takeBE(12); // worthless empty space and "stuff" :)
        int o_sierra_pos = sierra_pos = player.pos;
        sierra_next_section(player);

        while (player.peek(sierra_pos - 2) != 255) {
            sierra_next_section(player);
            ++player.subsongs;
        }

        if (subSong < 0 || subSong >= player.subsongs) {
            subSong = 0;
        }

        sierra_pos = o_sierra_pos;
        sierra_next_section(player);

        for (int i = 0; i != subSong; ++i) {
            sierra_next_section(player);
        }
    }

    @Override
    void init(Opl3Synthesizer synthesizer) {
        synthesizer.instruments = smyinsbank;
        synthesizer.adlib.style = Adlib.SIERRA_STYLE | Adlib.MIDI_STYLE; // advanced sierra tunes use volume;
    }
}

/* */
