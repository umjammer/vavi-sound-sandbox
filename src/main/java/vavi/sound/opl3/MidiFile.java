/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.opl3;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

import vavi.sound.midi.opl3.Opl3Synthesizer.Context;
import vavi.sound.opl3.MidPlayer.MidiTypeFile;


/**
 * MidiFile.
 * <p>
 * support only format 0 SMF.
 * <li> if you want to use this device, set system property "vavi.sound.opl3.MidiFile" true
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/25 umjammer initial version <br>
 */
class MidiFile extends MidiTypeFile {

    private static final Logger logger = Logger.getLogger(MidiFile.class.getName());

    @Override
    int markSize() {
        return 10;
    }

    @Override
    boolean matchFormatImpl(DataInputStream dis) throws IOException {
        if (!Boolean.parseBoolean(System.getProperty("vavi.sound.opl3.MidiFile", "false"))) {
logger.info("vavi.sound.opl3.MidiFile: false");
            return false;
        }
logger.info("use vavi.sound.opl3.MidiFile");
        byte[] chunkType = new byte[4];
        dis.readFully(chunkType);
        dis.skipBytes(4);
        int format = dis.readUnsignedShort();
logger.info("format: " + format);
        return Arrays.equals("MThd".getBytes(), chunkType) && format == 0;
    }

    @Override
    void rewind(int subSong, MidPlayer player) throws IOException {
        player.tins = 128;
        player.takeBE(4 + 4 + 2 + 2); // skip header
        player.deltas = player.takeBE(2);
        logger.fine(String.format("deltas: %d", player.deltas));
        player.takeBE(4);

        player.tracks[0].on = true;
        player.tracks[0].tend = player.takeBE(4);
        player.tracks[0].spos = player.pos;
        logger.info(String.format("tracklen: %d", player.tracks[0].tend));
    }

    @Override
    public void init(Context context) {
    }
}
