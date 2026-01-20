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
import vavi.sound.midi.opl3.Opl3Synthesizer.Context;
import vavi.sound.opl3.MidPlayer.MidiTypeFile;

import static java.lang.System.getLogger;


/**
 * MidiFile.
 * <p>
 * support only format 0 SMF.
 * <p>
 * system properties
 * <ul>
 *  <li>"vavi.sound.opl3.MidiFile" ... if you want to use this device, set {@code true}</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/25 umjammer initial version <br>
 */
class MidiFile extends MidiTypeFile {

    private static final Logger logger = getLogger(MidiFile.class.getName());

    @Override
    int markSize() {
        return 10;
    }

    @Override
    boolean matchFormatImpl(DataInputStream dis) throws IOException {
        if (!Boolean.parseBoolean(System.getProperty("vavi.sound.opl3.MidiFile", "false"))) {
logger.log(Level.DEBUG, "vavi.sound.opl3.MidiFile: false");
            return false;
        }
logger.log(Level.DEBUG, "use vavi.sound.opl3.MidiFile");
        byte[] chunkType = new byte[4];
        dis.readFully(chunkType);
        dis.skipBytes(4);
        int format = dis.readUnsignedShort();
logger.log(Level.DEBUG, "format: " + format);
        return Arrays.equals("MThd".getBytes(), chunkType) && format == 0;
    }

    @Override
    void rewind(int subSong, MidPlayer player) throws IOException {
        if (!(this instanceof LucasFile)) {
            player.tins = 128;
            player.takeBE(1);
        }
        player.takeBE(3 + 4 + 2 + 2); // skip header
        player.deltas = player.takeBE(2);
logger.log(Level.DEBUG, "deltas: %d".formatted(player.deltas));
        player.takeBE(4);

        player.tracks[0].on = true;
        player.tracks[0].tend = player.takeBE(4);
        player.tracks[0].spos = player.pos;
logger.log(Level.DEBUG, "tracklen: %d".formatted(player.tracks[0].tend));
    }

    protected Context context;

    @Override
    public void init(Context context) {
        this.context = context;
    }
}
