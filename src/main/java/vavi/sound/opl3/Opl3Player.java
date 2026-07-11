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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;

import static java.lang.System.getLogger;


/**
 * Player base class.
 *
 * @author Simon Peter <dn.tlp@gmx.net>
 */
public abstract class Opl3Player {

    private static final Logger logger = getLogger(Opl3Player.class.getName());

    /** generic properties */
    protected Map<String, Object> props = new HashMap<>();

    /** YMF262 native output rate: 14.318180 MHz / 288 */
    public static final AudioFormat opl3 = new AudioFormat(49716.0f, 16, 2, true, false);

    public abstract AudioFileFormat.Type getType();

    public abstract AudioFormat.Encoding getEncoding();

    /** players using opl3 decoder */
    private static final List<Opl3Player> players = ServiceLoader.load(Opl3Player.class).stream().map(Provider::get).toList();

    public static Opl3Player getPlayer(AudioFormat.Encoding encoding) {
logger.log(Level.DEBUG, "encoding: " + encoding);
        return players.stream().filter(p -> p.getEncoding().equals(encoding)).findFirst().orElseThrow();
    }

    /** @param ext lower case w/o '.' */
    public static AudioFileFormat.Type getType(String ext) {
        return players.stream().filter(p -> p.getType().getExtension().contains(ext)).findFirst().orElseThrow().getType();
    }

    public static AudioFileFormat.Type getType(AudioFormat.Encoding encoding) {
        return players.stream().filter(p -> p.getEncoding().equals(encoding)).findFirst().orElseThrow().getType();
    }

    /** mark/reset will be done internally */
    public static AudioFormat.Encoding getEncoding(InputStream is) {
        return players.stream().filter(p -> p.matchFormat(is)).findFirst().orElseThrow().getEncoding();
    }

    public static List<Encoding> getEncodings() {
        return players.stream().map(Opl3Player::getEncoding).toList();
    }

    private final OPL3 opl;

    protected Opl3Player() {
        opl = new OPL3();
    }

    /**
     * Optional register-write log in adplug's playertest format
     * ("reg <- val" in hex), for comparison against the reference dumps
     * in adplug's test/testref directory. Null (off) by default.
     */
    public static java.io.PrintStream regLog;

    protected void write(int array, int address, int data) {
        if (regLog != null) regLog.printf("%x <- %x%n", ((array & 1) << 8) | (address & 0xff), data & 0xff);
        opl.write(array, address, data);
    }

    /** must implement mark/reset inside this method */
    public abstract boolean matchFormat(InputStream is);

    public abstract void load(InputStream is) throws IOException;

    public byte[] read(int len) {
//logger.log(Level.WARNING, "Enter in read method");

        byte[] buf = new byte[len];

        for (int i = 0; i < len; i += 4) {
            short[] data = opl.read();
            // sum CHA+CHC / CHB+CHD then clip, so loud passages saturate instead of wrapping around
            int chA = data[0] + data[2];
            int chB = data[1] + data[3];
            if (chA > Short.MAX_VALUE) chA = Short.MAX_VALUE; else if (chA < Short.MIN_VALUE) chA = Short.MIN_VALUE;
            if (chB > Short.MAX_VALUE) chB = Short.MAX_VALUE; else if (chB < Short.MIN_VALUE) chB = Short.MIN_VALUE;
            buf[i] = (byte) (chA & 0xff);
            buf[i + 1] = (byte) ((chA >> 8) & 0xff);
            buf[i + 2] = (byte) (chB & 0xff);
            buf[i + 3] = (byte) ((chB >> 8) & 0xff);
        }
//logger.log(Level.TRACE, "read: " + len);
        return buf;
    }

    public abstract void rewind(int subSong) throws IOException;

    public abstract float getRefresh();

    public abstract boolean update() throws IOException;

    public abstract int getTotalMilliseconds();

    public void setProperties(Map<String, Object> props) {
        this.props.clear();
        this.props.putAll(props);
logger.log(Level.TRACE, "props: " + this.props);
    }

    public Map<String, Object> getProperties() {
        return this.props;
    }
}
