/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2005 Simon Peter <dn.tlp@gmx.net>, et al.
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
import java.util.Arrays;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioFileFormat.Type;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

/**
 * Coktel Vision ADL player.
 * Ported from adplug's coktel.cpp / coktel.h by Stas'M.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class CoktelPlayer extends ComposerPlayer {

    private static class AdlInst {
        final byte[] initial = new byte[ADLIB_INST_LEN];
        final byte[] modified = new byte[ADLIB_INST_LEN];
        int backend_index;
    }

    private int pos;
    private int size;
    private boolean songend;
    private boolean first_tick;
    private byte[] data;
    private int soundMode;
    private int nrTimbre;

    private int counter;
    private int ticks;
    private final int[] timbre = new int[MAX_VOICES];
    private AdlInst[] insts;
    private int modifyTimbre = 0xFF;

    public CoktelPlayer() {
        super();
    }

    @Override
    public int getTotalMilliseconds() {
        return 0;
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("AdLib Visual Composer: Coktel Vision", "adl");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("COKTEL");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            java.net.URI uri = vavi.sound.SoundUtil.getSource(bitStream);
            if (uri != null) {
                String path = uri.getPath();
                if (path != null && !path.toLowerCase().endsWith(".adl")) {
                    return false;
                }
            }
            bitStream.mark(60);
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
        int soundMode = is.read();
        int nrTimbre = is.read();
        int reserved = is.read();
        if (soundMode < 0 || nrTimbre < 0 || reserved < 0) return false;
        return soundMode <= 1 && nrTimbre != 0xff && reserved == 0;
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();
        if (buf.length < 3 + 56 + 1) {
            throw new IllegalArgumentException("file too short");
        }

        soundMode = buf[0] & 0xff;
        nrTimbre = buf[1] & 0xff;
        int reserved = buf[2] & 0xff;

        if (soundMode > 1 || nrTimbre == 0xff || reserved != 0 ||
            buf.length < (3 + (nrTimbre + 1) * 56 + 1)) {
            throw new IllegalArgumentException("invalid header or truncated file");
        }

        int timbreCount = nrTimbre + 1;
        insts = new AdlInst[timbreCount];
        int offset = 3;
        for (int i = 0; i < timbreCount; i++) {
            insts[i] = new AdlInst();
            for (int j = 0; j < ADLIB_INST_LEN; j++) {
                int val = (buf[offset] & 0xff) | ((buf[offset + 1] & 0xff) << 8);
                offset += 2;
                insts[i].initial[j] = (byte) (val & 0xff);
            }
            insts[i].backend_index = -1;
        }

        size = buf.length - 3 - timbreCount * 56;
        data = new byte[size];
        System.arraycopy(buf, offset, data, 0, size);

        rewind(0);
    }

    @Override
    protected void frontend_rewind(int subsong) {
        pos = 0;
        songend = false;
        first_tick = false;

        SetRhythmMode(soundMode);

        for (int i = 0; i < insts.length; i++) {
            System.arraycopy(insts[i].initial, 0, insts[i].modified, 0, ADLIB_INST_LEN);
            insts[i].backend_index = load_instrument_data(insts[i].initial, ADLIB_INST_LEN);
        }

        Arrays.fill(timbre, 0);

        int numVoices = (soundMode != 0) ? kNumPercussiveVoices : kNumMelodicVoices;
        for (int i = 0; i < numVoices; i++) {
            SetInstrument(i, insts[timbre[i]].backend_index);
            SetVolume(i, 127);
        }

        counter = 0;
        ticks = 0;
        modifyTimbre = 0xFF;
    }

    private void executeCommand() {
        int status = data[pos++] & 0xff;
        if (status == 0xFF) { // COK_END_OF_SONG
            pos = size;
        } else if (status == 0xFE) { // COK_SET_MOD_TIMBRE
            modifyTimbre = data[pos++] & 0xff;
        } else if (status > 0xD0) { // COK_MODIFY_TIMBRE
            int note = data[pos++] & 0xff;
            int val = data[pos++] & 0xff;

            if (insts != null && modifyTimbre != 0xFF && modifyTimbre < insts.length) {
                insts[modifyTimbre].modified[note] = (byte) val;
                insts[modifyTimbre].backend_index = load_instrument_data(insts[modifyTimbre].modified, ADLIB_INST_LEN);

                int numVoices = (soundMode != 0) ? kNumPercussiveVoices : kNumMelodicVoices;
                for (int i = 0; i < numVoices; i++) {
                    if (timbre[i] == modifyTimbre) {
                        SetInstrument(i, insts[modifyTimbre].backend_index);
                    }
                }
            }
        } else {
            int voice = status & 0xF;
            int cmd = status & 0xF0;

            switch (cmd) {
                case 0x00 -> { // COK_NOTE_ON_VOL
                    int note = data[pos++] & 0xff;
                    int val = data[pos++] & 0xff;
                    if (voice >= MAX_VOICES) break;
                    SetVolume(voice, val);
                    NoteOn(voice, note);
                }
                case 0x80 -> { // COK_NOTE_OFF
                    if (voice >= MAX_VOICES) break;
                    NoteOff(voice);
                }
                case 0x90 -> { // COK_NOTE_ON
                    int note = data[pos++] & 0xff;
                    if (voice >= MAX_VOICES) break;
                    NoteOn(voice, note);
                }
                case 0xA0 -> { // COK_PITCH_BEND
                    int pitch = (data[pos++] & 0xff) << 7;
                    if (voice >= MAX_VOICES) break;
                    ChangePitch(voice, pitch);
                }
                case 0xB0 -> { // COK_VOLUME_SLIDE
                    int val = data[pos++] & 0xff;
                    if (voice >= MAX_VOICES) break;
                    SetVolume(voice, val);
                }
                case 0xC0 -> { // COK_TIMBRE_CHANGE
                    int val = data[pos++] & 0xff;
                    if (voice >= MAX_VOICES) break;
                    if (insts != null && val < insts.length) {
                        timbre[voice] = val;
                        SetInstrument(voice, insts[val].backend_index);
                    }
                }
                default -> pos = size;
            }
        }
    }

    @Override
    public boolean update() throws IOException {
        if (pos >= size) {
            rewind(0);
            songend = true;
        }

        if (counter == 0) {
            ticks = data[pos++] & 0xff;
            if ((ticks & 0x80) != 0) {
                ticks = ((ticks & ~0x80) << 8) | (data[pos++] & 0xff);
            }
            if (ticks != 0 && !first_tick) {
                ticks = 0;
                first_tick = true;
            }
        }

        counter++;
        if (counter >= ticks) {
            counter = 0;
            while (pos < size) {
                executeCommand();
                if (pos >= size) {
                    return false;
                } else if (data[pos] == 0) {
                    pos++;
                } else {
                    break;
                }
            }
        }

        return !songend;
    }

    @Override
    public float getRefresh() {
        return 1000.0f;
    }
}
