/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2007 Simon Peter, <dn.tlp@gmx.net>, et al.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * player.h - Replayer base class, by Simon Peter <dn.tlp@gmx.net>
 */

package org.uva.emulation;

import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;

import com.cozendey.opl3.OPL3;

import vavi.util.Debug;


abstract class Opl3Player {

    /** decoder database */
    enum FileType {
        MID(Opl3Encoding.MID, new MidPlayer()),
        DRO1(Opl3Encoding.DRO1, new DroPlayer(true)),
        DRO2(Opl3Encoding.DRO2, new Dro2Player(true));
        AudioFormat.Encoding encoding;
        Opl3Player player;
        FileType(AudioFormat.Encoding encoding, Opl3Player player) {
            this.encoding = encoding;
            this.player = player;
        }
        static Opl3Player getPlayer(AudioFormat.Encoding encoding) {
Debug.println("encoding: " + encoding);
            return Arrays.stream(values()).filter(e -> e.encoding == encoding).findFirst().get().player;
        }
    }

    private OPL3 opl;

    protected Opl3Player() {
        opl = new OPL3();
    }

    protected void write(int array, int address, int data) {
        opl.write(array, address, data);
    }

    public abstract void load(byte[] file) throws IOException;

    public byte[] read(int len) {
//LOGGER.warning("Enter in read method");

        byte[] buf = new byte[len];

        for (int i = 0; i < len; i += 4) {
            short[] data = opl.read();
            short chA = data[0];
            short chB = data[1];
            buf[i] = (byte) (chA & 0xff);
            buf[i + 1] = (byte) (chA >> 8 & 0xff);
            buf[i + 2] = (byte) (chB & 0xff);
            buf[i + 3] = (byte) (chB >> 8 & 0xff);
        }
//LOGGER.info("read: " + len);
      return buf;
    }

    public abstract void rewind(int subSong);

    public abstract float getRefresh();

    public abstract boolean update();

    public abstract int getTotalMiliseconds();
}
