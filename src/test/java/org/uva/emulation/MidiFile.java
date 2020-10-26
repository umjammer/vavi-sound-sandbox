/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.uva.emulation;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.logging.Logger;

import org.uva.emulation.MidPlayer.FileType;
import org.uva.emulation.MidPlayer.MidiTypeFile;

import vavi.util.StringUtil;


/**
 * MidiFile.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/25 umjammer initial version <br>
 */
class MidiFile extends MidiTypeFile {
    static Logger logger = Logger.getLogger(MidiFile.class.getName());

    int markSize() {
        return 4;
    }
    boolean matchFormatImpl(DataInputStream dis) throws IOException {
        return dis.readUnsignedByte() == 'M' &&
                dis.readUnsignedByte() == 'T' &&
                dis.readUnsignedByte() == 'h' &&
                dis.readUnsignedByte() == 'd';
    }

    @Override
    void rewind(int subSong, MidPlayer player) throws IOException {
logger.info("\n" + StringUtil.getDump(player.data, 0, 128));
        if (player.type != FileType.LUCAS) {
            player.tins = 128;
        }
        player.takeBE(12); // skip header
        player.deltas = player.takeBE(2);
        logger.fine(String.format("deltas: %d", player.deltas));
        player.takeBE(4);

        player.tracks[0].on = true;
        player.tracks[0].tend = player.takeBE(4);
        player.tracks[0].spos = player.pos;
        logger.info(String.format("tracklen: %d", player.tracks[0].tend));
    }

    @Override
    void init(Opl3Synthesizer synthesizer) {
    }
}

/* */
