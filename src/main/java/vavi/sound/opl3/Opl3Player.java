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
 */

package vavi.sound.opl3;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;

import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;
import vavi.util.Debug;


/**
 * Player base class.
 *
 * @author Simon Peter <dn.tlp@gmx.net>
 */
public abstract class Opl3Player {

    /** TODO who defined 49700? */
    public static final AudioFormat opl3 = new AudioFormat(49700.0f, 16, 2, true, false);

    /** decoder database */
    public enum FileType {
        MID(Opl3Encoding.MID, Opl3FileFormatType.MID, new MidPlayer()),
        DRO1(Opl3Encoding.DRO1, Opl3FileFormatType.DRO1, new DroPlayer()),
        DRO2(Opl3Encoding.DRO2, Opl3FileFormatType.DRO2, new Dro2Player());
        AudioFormat.Encoding encoding;
        AudioFileFormat.Type type;
        Opl3Player player;
        FileType(AudioFormat.Encoding encoding, AudioFileFormat.Type type, Opl3Player player) {
            this.encoding = encoding;
            this.type = type;
            this.player = player;
        }
        public static Opl3Player getPlayer(AudioFormat.Encoding encoding) {
Debug.println("encoding: " + encoding);
            return Arrays.stream(values()).filter(e -> e.encoding == encoding).findFirst().get().player;
        }
        /** @param ext lower case w/o '.' */
        static AudioFileFormat.Type getType(String ext) {
            return Arrays.stream(values()).filter(e -> e.type.getExtension().contains(ext)).findFirst().get().type;
        }
        public static AudioFileFormat.Type getType(AudioFormat.Encoding encoding) {
            return Arrays.stream(values()).filter(e -> e.encoding == encoding).findFirst().get().type;
        }
        public static AudioFormat.Encoding getEncoding(InputStream is) {
            return Arrays.stream(values()).filter(e -> e.player.matchFormat(is)).findFirst().get().encoding;
        }
    }

    private OPL3 opl;

    protected Opl3Player() {
        opl = new OPL3();
    }

    protected void write(int array, int address, int data) {
        opl.write(array, address, data);
    }

    public abstract boolean matchFormat(InputStream is);

    public abstract void load(InputStream is) throws IOException;

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

    public abstract void rewind(int subSong) throws IOException;

    public abstract float getRefresh();

    public abstract boolean update() throws IOException;

    public abstract int getTotalMiliseconds();
}
