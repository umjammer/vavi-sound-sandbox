/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2007 Simon Peter <dn.tlp@gmx.net>, et al.
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
 * Faust Music Creator Loader.
 * Ported from adplug's fmc.cpp / fmc.h by Riven the Mage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class FmcPlayer extends ProtrackPlayer {

    private String songTitle = "";

    private static class FmcInstrument {
        int synthesis;
        int feedback;
        int mod_attack, mod_decay, mod_sustain, mod_release, mod_volume, mod_ksl, mod_freq_multi, mod_waveform, mod_sustain_sound, mod_ksr, mod_vibrato, mod_tremolo;
        int car_attack, car_decay, car_sustain, car_release, car_volume, car_ksl, car_freq_multi, car_waveform, car_sustain_sound, car_ksr, car_vibrato, car_tremolo;
        int pitch_shift;
        String name = "";
    }

    public FmcPlayer() {
        super();
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("Faust Music Creator", "fmc");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("FMC");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(64);
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
        byte[] sig = new byte[4];
        int read = is.read(sig);
        if (read < 4) return false;
        if (sig[0] != 'F' || sig[1] != 'M' || sig[2] != 'C' || sig[3] != '!') {
            return false;
        }
        byte[] title = new byte[21];
        int readTitle = is.read(title);
        if (readTitle < 21) return false;
        int numchan = is.read();
        return numchan >= 1 && numchan <= 32;
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();
        if (buf.length < 4 + 21 + 1) {
            throw new IllegalArgumentException("file too short");
        }

        if (buf[0] != 'F' || buf[1] != 'M' || buf[2] != 'C' || buf[3] != '!') {
            throw new IllegalArgumentException("invalid signature");
        }

        songTitle = readPascalString(buf, 4, 21);
        int numchan = buf[25] & 0xff;

        if (numchan < 1 || numchan > 32) {
            throw new IllegalArgumentException("invalid channel count");
        }

        reallocInstruments(32);
        reallocOrder(256);
        reallocPatterns(64, 64, numchan);
        initTrackord();

        int offset = 26;

        // load order
        if (offset + 256 > buf.length) {
            throw new IllegalArgumentException("truncated order table");
        }
        for (int i = 0; i < 256; i++) {
            order[i] = buf[offset++];
        }

        offset += 2; // ignore 2 bytes

        // load instruments
        FmcInstrument[] instruments = new FmcInstrument[32];
        for (int i = 0; i < 32; i++) {
            if (offset + 27 + 21 > buf.length) {
                throw new IllegalArgumentException("truncated instruments");
            }
            instruments[i] = new FmcInstrument();
            instruments[i].synthesis = buf[offset++] & 0xff;
            instruments[i].feedback = buf[offset++] & 0xff;

            instruments[i].mod_attack = buf[offset++] & 0xff;
            instruments[i].mod_decay = buf[offset++] & 0xff;
            instruments[i].mod_sustain = buf[offset++] & 0xff;
            instruments[i].mod_release = buf[offset++] & 0xff;
            instruments[i].mod_volume = buf[offset++] & 0xff;
            instruments[i].mod_ksl = buf[offset++] & 0xff;
            instruments[i].mod_freq_multi = buf[offset++] & 0xff;
            instruments[i].mod_waveform = buf[offset++] & 0xff;
            instruments[i].mod_sustain_sound = buf[offset++] & 0xff;
            instruments[i].mod_ksr = buf[offset++] & 0xff;
            instruments[i].mod_vibrato = buf[offset++] & 0xff;
            instruments[i].mod_tremolo = buf[offset++] & 0xff;

            instruments[i].car_attack = buf[offset++] & 0xff;
            instruments[i].car_decay = buf[offset++] & 0xff;
            instruments[i].car_sustain = buf[offset++] & 0xff;
            instruments[i].car_release = buf[offset++] & 0xff;
            instruments[i].car_volume = buf[offset++] & 0xff;
            instruments[i].car_ksl = buf[offset++] & 0xff;
            instruments[i].car_freq_multi = buf[offset++] & 0xff;
            instruments[i].car_waveform = buf[offset++] & 0xff;
            instruments[i].car_sustain_sound = buf[offset++] & 0xff;
            instruments[i].car_ksr = buf[offset++] & 0xff;
            instruments[i].car_vibrato = buf[offset++] & 0xff;
            instruments[i].car_tremolo = buf[offset++] & 0xff;

            instruments[i].pitch_shift = buf[offset++];

            instruments[i].name = readPascalString(buf, offset, 21);
            offset += 21;
        }

        // load tracks
        int[] convFx = { 0, 1, 2, 3, 4, 8, 255, 255, 255, 255, 26, 11, 12, 13, 14, 15 };
        int t = 0;
        for (int i = 0; i < 64; i++) {
            if (offset >= buf.length) break;

            for (int j = 0; j < numchan; j++) {
                for (int k = 0; k < 64; k++) {
                    if (offset + 3 > buf.length) break;
                    int byte0 = buf[offset++] & 0xff;
                    int byte1 = buf[offset++] & 0xff;
                    int byte2 = buf[offset++] & 0xff;

                    Tracks track = tracks[t][k];
                    track.note = byte0 & 0x7F;
                    track.inst = ((byte0 & 0x80) >> 3) + (byte1 >> 4) + 1;
                    track.command = convFx[byte1 & 0x0F];
                    track.param1 = byte2 >> 4;
                    track.param2 = byte2 & 0x0F;

                    // fix effects
                    if (track.command == 0x0E) { // Retrig
                        track.param1 = 3;
                    }
                    if (track.command == 0x1A) { // Volume Slide
                        if (track.param1 > track.param2) {
                            track.param1 -= track.param2;
                            track.param2 = 0;
                        } else {
                            track.param2 -= track.param1;
                            track.param1 = 0;
                        }
                    }
                }
                t++;
            }
        }

        // convert instruments
        for (int i = 0; i < 31; i++) {
            buildinst(i, instruments[i]);
        }

        activechan = (int) ((0xffffffffL >> (32 - numchan)) << (32 - numchan));
        nop = t / numchan;
        if (nop == 0) {
            throw new IllegalArgumentException("truncated file, no track data");
        }
        restartpos = 0;

        // order length
        for (length = 0; length < 256; length++) {
            int ordVal = order[length] & 0xff;
            if (ordVal >= 0xFE) {
                break;
            } else if (ordVal >= nop) {
                throw new IllegalArgumentException("invalid pattern index in order list");
            }
        }

        flags = Faust;

        rewind(0);
    }

    private void buildinst(int i, FmcInstrument instVal) {
        inst[i].data[0] = ((instVal.synthesis & 1) ^ 1);
        inst[i].data[0] |= ((instVal.feedback & 7) << 1);

        inst[i].data[3] = ((instVal.mod_attack & 15) << 4);
        inst[i].data[3] |= (instVal.mod_decay & 15);
        inst[i].data[5] = ((15 - (instVal.mod_sustain & 15)) << 4);
        inst[i].data[5] |= (instVal.mod_release & 15);
        inst[i].data[9] = (63 - (instVal.mod_volume & 63));
        inst[i].data[9] |= ((instVal.mod_ksl & 3) << 6);
        inst[i].data[1] = (instVal.mod_freq_multi & 15);
        inst[i].data[7] = (instVal.mod_waveform & 3);
        inst[i].data[1] |= ((instVal.mod_sustain_sound & 1) << 5);
        inst[i].data[1] |= ((instVal.mod_ksr & 1) << 4);
        inst[i].data[1] |= ((instVal.mod_vibrato & 1) << 6);
        inst[i].data[1] |= ((instVal.mod_tremolo & 1) << 7);

        inst[i].data[4] = ((instVal.car_attack & 15) << 4);
        inst[i].data[4] |= (instVal.car_decay & 15);
        inst[i].data[6] = ((15 - (instVal.car_sustain & 15)) << 4);
        inst[i].data[6] |= (instVal.car_release & 15);
        inst[i].data[10] = (63 - (instVal.car_volume & 63));
        inst[i].data[10] |= ((instVal.car_ksl & 3) << 6);
        inst[i].data[2] = (instVal.car_freq_multi & 15);
        inst[i].data[8] = (instVal.car_waveform & 3);
        inst[i].data[2] |= ((instVal.car_sustain_sound & 1) << 5);
        inst[i].data[2] |= ((instVal.car_ksr & 1) << 4);
        inst[i].data[2] |= ((instVal.car_vibrato & 1) << 6);
        inst[i].data[2] |= ((instVal.car_tremolo & 1) << 7);

        inst[i].slide = instVal.pitch_shift;
    }

    private String readPascalString(byte[] buf, int offset, int maxLength) {
        int len = 0;
        while (len < maxLength && offset + len < buf.length && buf[offset + len] != 0) {
            len++;
        }
        return new String(buf, offset, len, java.nio.charset.StandardCharsets.US_ASCII).trim();
    }

    @Override
    public float getRefresh() {
        return 50.0f;
    }
}
