/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.uva.emulation;

import java.io.DataInputStream;
import java.io.IOException;


/**
 * LucasFile.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/25 umjammer initial version <br>
 */
class LucasFile extends MidiFile {
    int markSize() {
        return 3;
    }

    boolean matchFormatImpl(DataInputStream dis) throws IOException {
        return dis.readUnsignedByte() == 'A' &&
            dis.readUnsignedByte() == 'D' &&
            dis.readUnsignedByte() == 'L';
    }

    @Override
    void rewind(int subSong, MidPlayer player) throws IOException {
        player.takeBE(24); //skip junk and get to the midi.
        // note: no break, we go right into midi headers...
        super.rewind(subSong, player);
    }

    @Override
    void init(Opl3Synthesizer synthesizer) {
        synthesizer.adlib.style = Adlib.LUCAS_STYLE | Adlib.MIDI_STYLE;
        super.init(synthesizer);
    }
}

/* */
