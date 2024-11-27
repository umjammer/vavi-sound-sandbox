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
import java.net.URI;
import java.nio.file.Path;

import vavi.sound.midi.opl3.Opl3Synthesizer.Context;

import static java.lang.System.getLogger;


/**
 * AdvancedSierraFile. (SCI)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/26 umjammer initial version <br>
 */
class AdvancedSierraFile extends SierraFile {

    private static final Logger logger = getLogger(AdvancedSierraFile.class.getName());

    @Override
    int markSize() {
        return 3;
    }

    @Override
    boolean matchFormatImpl(DataInputStream dis) throws IOException {
        return dis.readUnsignedByte() == 0x84 &&
                dis.readUnsignedByte() == 0 &&
                dis.readUnsignedByte() == 0xf0; // advanced sierra flag
    }

    private int sierraPos;

    private void sierra_next_section(MidPlayer player) throws IOException {
        for (int t = 0; t < 16; ++t) {
            player.tracks[t].on = false;
        }

logger.log(Level.INFO, "next adv sierra section:");
        player.pos = sierraPos;

        int t = 0;
        for (int i = 0; i != 255; i = player.takeBE(1)) {
            player.takeBE(1);
            player.tracks[t].on = true;
            player.tracks[t].spos = player.takeBE(1);
            player.tracks[t].spos += (player.takeBE(1) << 8) + 4; // 4 best usually +3? not 0,1,2 or 5
            player.tracks[t].tend = player.flen;
            player.tracks[t].iwait = 0;
            player.tracks[t].pv = 0;
logger.log(Level.INFO, String.format("track %d starts at %x", t, player.tracks[t].spos));
            t++;
            player.takeBE(2);
        }

        player.takeBE(2);
        player.deltas = 32;
        sierraPos = player.pos;
        player.fwait = 0.0F;
        player.doing = true;
    }

    @Override
    void rewind(int subSong, MidPlayer player) throws IOException {
        this.uri = (URI) player.getProperties().get("uri");
logger.log(Level.DEBUG, "uri: " + uri);
        if (uri != null) loadSierraIns(uri);

        player.tins = stins;
        player.deltas = 32;
        player.takeBE(12); // worthless empty space and "stuff" :)
        int o_sierra_pos = sierraPos = player.pos;
        sierra_next_section(player);

        while (player.peek(sierraPos - 2) != 255) {
            sierra_next_section(player);
            ++player.subsongs;
        }

        if (subSong < 0 || subSong >= player.subsongs) {
            subSong = 0;
        }

        sierraPos = o_sierra_pos;
        sierra_next_section(player);

        for (int i = 0; i != subSong; ++i) {
            sierra_next_section(player);
        }
    }

    @Override
    public void init(Context context) {
        context.instruments(smyinsbank);
        context.adlib().style = Adlib.SIERRA_STYLE | Adlib.MIDI_STYLE; // advanced sierra tunes use volume;
    }
}
