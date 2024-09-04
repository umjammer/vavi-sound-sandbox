/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.opl3;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import vavi.sound.midi.opl3.Opl3Soundbank;
import vavi.sound.midi.opl3.Opl3Synthesizer.Context;


/**
 * AdvancedSierraFile. (SCI)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/26 umjammer initial version <br>
 */
class AdvancedSierraFile extends SierraFile {

    private static final Logger logger = Logger.getLogger(MidiFile.class.getName());

    private URI uri;

    @Override
    int markSize() {
        return 3;
    }

    @Override
    boolean matchFormatImpl(DataInputStream dis) throws IOException {
        if (uri != null) loadSierraIns(Path.of(uri));
        return dis.readUnsignedByte() == 0x84 &&
                dis.readUnsignedByte() == 0 &&
                dis.readUnsignedByte() == 0xf0;
    }

    private int sierraPos;

    private static int[] fromSierra(int[] buf) {
        int[] x = new int[11];
        x[0] = buf[9] * 0x80 + buf[10] * 0x40 + buf[5] * 0x20 + buf[11] * 0x10 + buf[1];
        x[1] = buf[22] * 0x80 + buf[23] * 0x40 + buf[18] * 0x20 + buf[24] * 0x10 + buf[14];
        x[2] = (buf[0] << 6) + buf[8];
        x[3] = (buf[13] << 6) + buf[21];
        x[4] = (buf[3] << 4) + buf[6];
        x[5] = (buf[16] << 4) + buf[19];
        x[6] = (buf[4] << 4) + buf[7];
        x[7] = (buf[17] << 4) + buf[20];
        x[8] = buf[26];
        x[9] = buf[27];
        x[10] = (buf[2] << 1) + (1 - (buf[12] & 1));
        return x;
    }

    private void loadSierraIns(Path path) throws IOException {

        Path patch = path.getParent().resolve("patch.003");
        DataInputStream dis = new DataInputStream(Files.newInputStream(patch.toFile().toPath()));
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

                smyinsbank[p] = Opl3Soundbank.newInstrument(0, p, "sierra." + p, fromSierra(buf));

                ++stins;
            }

            dis.skipBytes(2);
        }

        dis.close();
    }

    private void sierra_next_section(MidPlayer player) throws IOException {
        for (int t = 0; t < 16; ++t) {
            player.tracks[t].on = false;
        }

logger.info("next adv sierra section:");
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
logger.info(String.format("track %d starts at %x", t, player.tracks[t].spos));
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
