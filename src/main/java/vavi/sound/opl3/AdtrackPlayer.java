/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2003 Simon Peter <dn.tlp@gmx.net>, et al.
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
 */

package vavi.sound.opl3;

import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioFileFormat.Type;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

/**
 * Adlib Tracker 1.0 Loader.
 * Ported from adplug's adtrack.cpp / adtrack.h by Simon Peter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class AdtrackPlayer extends ProtrackPlayer {

    private static class AdTrackInst {
        static class Op {
            int appampmod, appvib, maintsuslvl, keybscale, octave,
                freqrisevollvldn, softness, attack, decay, release, sustain,
                feedback, waveform;
        }
        final Op[] op = { new Op(), new Op() };
    }

    public AdtrackPlayer() {
        super();
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("Adlib Tracker 1.0", "sng");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("ADTRACK");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(65536);
            return matchFormatImpl(bitStream);
        } catch (Exception e) {
            return false;
        } finally {
            try {
                bitStream.reset();
            } catch (IOException ignored) {}
        }
    }

    protected boolean matchFormatImpl(InputStream is) throws IOException {
        return is.available() == 36000;
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] songBuf = is.readAllBytes();
        if (songBuf.length != 36000) {
            throw new IllegalArgumentException("invalid file size");
        }

        java.net.URI uri = null;
        if (props.containsKey("uri")) {
            Object o = props.get("uri");
            if (o instanceof java.net.URI) {
                uri = (java.net.URI) o;
            } else if (o instanceof String) {
                try {
                    uri = java.net.URI.create((String) o);
                } catch (Exception ignored) {}
            }
        }

        InputStream instf = null;
        if (uri != null && "file".equals(uri.getScheme())) {
            String path = uri.getPath();
            if (path != null) {
                int dot = path.lastIndexOf('.');
                if (dot != -1) {
                    String instPath = path.substring(0, dot) + ".ins";
                    java.io.File instFile = new java.io.File(instPath);
                    if (instFile.exists() && instFile.length() == 468) {
                        instf = new java.io.FileInputStream(instFile);
                    }
                }
            }
        }
        if (instf == null) {
            throw new IllegalArgumentException("missing instruments file");
        }

        reallocPatterns(1, 1000, 9);
        reallocInstruments(9);
        reallocOrder(1);
        initTrackord();
        flags = NoKeyOn;

        order[0] = 0;
        length = 1;
        restartpos = 0;
        bpm = 120;
        initspeed = 3;

        byte[] instBuf = new byte[468];
        int readIns = instf.read(instBuf);
        instf.close();
        if (readIns < 468) {
            throw new IllegalArgumentException("corrupted instruments file");
        }

        int offset = 0;
        for (int i = 0; i < 9; i++) {
            AdTrackInst myinst = new AdTrackInst();
            for (int j = 0; j < 2; j++) {
                myinst.op[j].appampmod = readShortLE(instBuf, offset);
                myinst.op[j].appvib = readShortLE(instBuf, offset + 2);
                myinst.op[j].maintsuslvl = readShortLE(instBuf, offset + 4);
                myinst.op[j].keybscale = readShortLE(instBuf, offset + 6);
                myinst.op[j].octave = readShortLE(instBuf, offset + 8);
                myinst.op[j].freqrisevollvldn = readShortLE(instBuf, offset + 10);
                myinst.op[j].softness = readShortLE(instBuf, offset + 12);
                myinst.op[j].attack = readShortLE(instBuf, offset + 14);
                myinst.op[j].decay = readShortLE(instBuf, offset + 16);
                myinst.op[j].release = readShortLE(instBuf, offset + 18);
                myinst.op[j].sustain = readShortLE(instBuf, offset + 20);
                myinst.op[j].feedback = readShortLE(instBuf, offset + 22);
                myinst.op[j].waveform = readShortLE(instBuf, offset + 24);
                offset += 26;
            }
            convertInstrument(i, myinst);
        }

        int songOffset = 0;
        for (int rwp = 0; rwp < 1000; rwp++) {
            for (int chp = 0; chp < 9; chp++) {
                char char0 = (char) (songBuf[songOffset] & 0xff);
                char char1 = (char) (songBuf[songOffset + 1] & 0xff);
                int octave = songBuf[songOffset + 2] & 0xff;
                songOffset += 4;

                int pnote = 0;
                switch (char0) {
                    case 'C' -> pnote = (char1 == '#') ? 2 : 1;
                    case 'D' -> pnote = (char1 == '#') ? 4 : 3;
                    case 'E' -> pnote = 5;
                    case 'F' -> pnote = (char1 == '#') ? 7 : 6;
                    case 'G' -> pnote = (char1 == '#') ? 9 : 8;
                    case 'A' -> pnote = (char1 == '#') ? 11 : 10;
                    case 'B' -> pnote = 12;
                    case '\0' -> {
                        if (char1 == '\0') {
                            tracks[chp][rwp].note = 127;
                        } else {
                            throw new IllegalArgumentException("corrupted song data");
                        }
                    }
                    default -> {
                        throw new IllegalArgumentException("corrupted song data");
                    }
                }
                if (char0 != '\0') {
                    tracks[chp][rwp].note = pnote + octave * 12;
                    tracks[chp][rwp].inst = chp + 1;
                }
            }
        }

        rewind(0);
    }

    private void convertInstrument(int n, AdTrackInst instVal) {
        int Carrier = 1;
        int Modulator = 0;

        inst[n].data[2] = instVal.op[Carrier].appampmod != 0 ? 1 << 7 : 0;
        inst[n].data[2] += instVal.op[Carrier].appvib != 0 ? 1 << 6 : 0;
        inst[n].data[2] += instVal.op[Carrier].maintsuslvl != 0 ? 1 << 5 : 0;
        inst[n].data[2] += instVal.op[Carrier].keybscale != 0 ? 1 << 4 : 0;
        inst[n].data[2] += (instVal.op[Carrier].octave + 1) & 0xffff;

        inst[n].data[1] = instVal.op[Modulator].appampmod != 0 ? 1 << 7 : 0;
        inst[n].data[1] += instVal.op[Modulator].appvib != 0 ? 1 << 6 : 0;
        inst[n].data[1] += instVal.op[Modulator].maintsuslvl != 0 ? 1 << 5 : 0;
        inst[n].data[1] += instVal.op[Modulator].keybscale != 0 ? 1 << 4 : 0;
        inst[n].data[1] += (instVal.op[Modulator].octave + 1) & 0xffff;

        inst[n].data[10] = (instVal.op[Carrier].freqrisevollvldn & 3) << 6;
        inst[n].data[10] += instVal.op[Carrier].softness & 63;
        inst[n].data[9] = (instVal.op[Modulator].freqrisevollvldn & 3) << 6;
        inst[n].data[9] += instVal.op[Modulator].softness & 63;

        inst[n].data[4] = (instVal.op[Carrier].attack & 0x0f) << 4;
        inst[n].data[4] += instVal.op[Carrier].decay & 0x0f;
        inst[n].data[3] = (instVal.op[Modulator].attack & 0x0f) << 4;
        inst[n].data[3] += instVal.op[Modulator].decay & 0x0f;

        inst[n].data[6] = (instVal.op[Carrier].release & 0x0f) << 4;
        inst[n].data[6] += instVal.op[Carrier].sustain & 0x0f;
        inst[n].data[5] = (instVal.op[Modulator].release & 0x0f) << 4;
        inst[n].data[5] += instVal.op[Modulator].sustain & 0x0f;

        inst[n].data[0] = (instVal.op[Carrier].feedback & 7) << 1;

        inst[n].data[8] = instVal.op[Carrier].waveform & 3;
        inst[n].data[7] = instVal.op[Modulator].waveform & 3;
    }

    private int readShortLE(byte[] buf, int offset) {
        return (buf[offset] & 0xff) | ((buf[offset + 1] & 0xff) << 8);
    }

    @Override
    public float getRefresh() {
        return 18.2f;
    }


}
