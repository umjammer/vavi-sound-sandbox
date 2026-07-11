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
 */

package vavi.sound.opl3;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;
import vavi.sound.SoundUtil;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

/**
 * Softstar RIX OPL Music Format Player.
 * Ported from adplug's rix.cpp / rix.h by palxex and BSPAL.
 *
 * The original is 16-bit assembly-derived code; the mixed
 * unsigned short / signed short arithmetic is reproduced with explicit
 * masks and casts.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class RixPlayer extends Opl3Player {

    private static final int[] adflag = { 0, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 1 };
    private static final int[] reg_data = { 0, 1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13, 16, 17, 18, 19, 20, 21 };
    private static final int[] ad_C0_offs = { 0, 1, 2, 0, 1, 2, 3, 4, 5, 3, 4, 5, 6, 7, 8, 6, 7, 8 };
    private static final int[] modify = {
        0, 3, 1, 4, 2, 5, 6, 9, 7, 10, 8, 11, 12, 15, 13, 16, 14, 17, 12,
        15, 16, 0, 14, 0, 17, 0, 13, 0
    };
    private static final int[] bd_reg_data = {
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x08, 0x04, 0x02, 0x01,
        0x00, 0x01, 0x01, 0x03, 0x0F, 0x05, 0x00, 0x01, 0x03, 0x0F, 0x00,
        0x00, 0x00, 0x01, 0x00, 0x00, 0x01, 0x01, 0x0F, 0x07, 0x00, 0x02,
        0x04, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0A,
        0x04, 0x00, 0x08, 0x0C, 0x0B, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,
        0x00, 0x00, 0x0D, 0x04, 0x00, 0x06, 0x0F, 0x00, 0x00, 0x00, 0x00,
        0x01, 0x00, 0x00, 0x0C, 0x00, 0x0F, 0x0B, 0x00, 0x08, 0x05, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04, 0x00, 0x0F, 0x0B, 0x00,
        0x07, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00,
        0x0F, 0x0B, 0x00, 0x05, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x01, 0x00, 0x0F, 0x0B, 0x00, 0x07, 0x05, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00
    };

    private int flagMkf;
    private byte[] fileBuffer;
    /** offset of the current subsong within fileBuffer */
    private int rixOff;
    private final int[] f_buffer = new int[300];
    private final int[] a0b0_data2 = new int[11];
    private final int[] a0b0_data3 = new int[18];
    private final int[] a0b0_data4 = new int[18];
    private final int[] a0b0_data5 = new int[96];
    private final int[] addrs_head = new int[96];
    private final int[] insbuf = new int[28];
    private final int[] displace = new int[11];
    private final int[][] reg_bufs = new int[18][14];
    private long pos, length;

    private final int[] for40reg = new int[18];
    private long I;
    private int mus_block;
    private int ins_block;
    private int rhythm;
    private int music_on;
    private int pause_flag;
    private int band;
    private int band_low;
    private int e0_reg_flag;
    private int bd_modify;
    private int sustain;
    private int play_end;

    @Override
    public Type getType() {
        return new Opl3FileFormatType("Softstar RIX OPL Music Format", "rix");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("RIX");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            URI uri = SoundUtil.getSource(bitStream);
            boolean mkf = false;
            if (uri != null) {
                String path = uri.getPath();
                if (path != null && path.toLowerCase().endsWith(".mkf")) {
                    mkf = true;
                }
            }
            bitStream.mark(Integer.MAX_VALUE);
            byte[] buf = bitStream.readAllBytes();
            int off = 0;
            if (mkf) {
                if (buf.length < 4) return false;
                off = u16b(buf, 0) | (u16b(buf, 2) << 16);
            }
            return off >= 0 && off + 2 <= buf.length &&
                    (u16b(buf, off)) == 0x55aa;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                bitStream.reset();
            } catch (IOException ignored) {}
        }
    }

    @Override
    public int getTotalMilliseconds() {
        return 0;
    }

    @Override
    public void load(InputStream is) throws IOException {
        URI uri = SoundUtil.getSource(is);
        flagMkf = 0;
        if (uri != null) {
            String path = uri.getPath();
            if (path != null && path.toLowerCase().endsWith(".mkf")) {
                flagMkf = 1;
            }
        }
        byte[] buf = is.readAllBytes();
        int off = 0;
        if (flagMkf != 0) {
            off = (int) u32(buf, 0);
        }
        if (off + 2 > buf.length || u16b(buf, off) != 0x55aa) {
            throw new IllegalArgumentException("invalid signature");
        }
        fileBuffer = buf;
        length = pos = buf.length;
        if (flagMkf == 0) {
            rixOff = 0;
        }
        rewind(0);
    }

    @Override
    public boolean update() {
        int08hEntry();
        return play_end == 0;
    }

    @Override
    public void rewind(int subSong) {
        I = 0;
        mus_block = 0;
        ins_block = 0;
        rhythm = 0;
        music_on = 0;
        pause_flag = 0;
        band = 0;
        band_low = 0;
        e0_reg_flag = 0;
        bd_modify = 0;
        sustain = 0;
        play_end = 0;

        java.util.Arrays.fill(f_buffer, 0);
        java.util.Arrays.fill(a0b0_data2, 0);
        java.util.Arrays.fill(a0b0_data3, 0);
        java.util.Arrays.fill(a0b0_data4, 0);
        java.util.Arrays.fill(a0b0_data5, 0);
        java.util.Arrays.fill(addrs_head, 0);
        java.util.Arrays.fill(insbuf, 0);
        java.util.Arrays.fill(displace, 0);
        for (int[] rb : reg_bufs) {
            java.util.Arrays.fill(rb, 0);
        }
        java.util.Arrays.fill(for40reg, 0x7F);

        if (flagMkf != 0 && subSong >= 0) {
            long i, offset, next = 0, tableEnd;
            offset = u32(fileBuffer, 0);
            tableEnd = offset / 4;
            for (i = 1; i < tableEnd; i++) {
                next = u32(fileBuffer, (int) (4 * i));
                if (next != offset) {
                    if (--subSong < 0) break;
                    offset = next;
                }
            }
            // fix up bad/unknown offsets to end of file_buffer
            if (offset > pos) offset = pos;
            if (i >= tableEnd || next > pos || next < offset) next = pos;
            // start and length of the subsong
            rixOff = (int) offset;
            length = next - offset;
        }
        for (int i = 0; i < 256; i++) {
            write(0, i, 0);
        }
        write(0, 1, 32); // go to OPL2 mode
        adInitial();
        dataInitial();
    }

    @Override
    public float getRefresh() {
        return 70.0f;
    }

    // ---- implementation

    /** rix data byte (0 when out of bounds) */
    private int rb(long off) {
        int i = rixOff + (int) off;
        return i >= 0 && i < fileBuffer.length ? fileBuffer[i] & 0xff : 0;
    }

    private void dataInitial() {
        if (0x0D < length) {
            rhythm = rb(2);
            mus_block = (rb(0x0D) << 8) + rb(0x0C);
            ins_block = (rb(0x09) << 8) + rb(0x08);
            I = mus_block + 1;
        } else {
            I = mus_block = (int) length; // file too short; will stop playing immediately
        }
        if (rhythm != 0) {
            adA0b0lReg_(8, 0x18, 0);
            adA0b0lReg_(7, 0x1F, 0);
        }
        bd_modify = 0;
        band = 0;
        music_on = 1;
    }

    private void adInitial() {
        int k = 0;
        for (int i = 0; i < 25; i++) {
            long res = ((long) i * 24 + 10000) * 52088 / 250000 * 0x24000 / 0x1B503;
            f_buffer[i * 12] = (((int) (res & 0xffff)) + 4) >> 3;
            for (int t = 1; t < 12; t++) {
                res = (long) (res * 1.06) & 0xffffffffL;
                f_buffer[i * 12 + t] = (((int) (res & 0xffff)) + 4) >> 3;
            }
        }
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 12; j++) {
                a0b0_data5[k] = i;
                addrs_head[k] = j;
                k++;
            }
        }
        e0_reg_flag = 0x20;
    }

    private void adBop(int reg, int value) {
        write(0, reg & 0xff, value & 0xff);
    }

    private void int08hEntry() {
        int bandSus = 1;
        while (bandSus != 0) {
            if (sustain <= 0) {
                bandSus = rixProc();
                if (bandSus != 0) {
                    sustain += bandSus;
                } else {
                    play_end = 1;
                    break;
                }
            } else {
                sustain -= 14; // aging
                break;
            }
        }
    }

    private int rixProc() {
        int ctrl;
        if (music_on == 0 || pause_flag == 1) return 0;
        band = 0;
        while (I < length && rb(I) != 0x80) {
            band_low = rb(I - 1);
            ctrl = rb(I);
            I += 2;
            switch (ctrl & 0xF0) {
            case 0x90:
                rixGetIns();
                rix90Pro(ctrl & 0x0F);
                break;
            case 0xA0:
                rixA0Pro(ctrl & 0x0F, band_low << 6);
                break;
            case 0xB0:
                rixB0Pro(ctrl & 0x0F, band_low);
                break;
            case 0xC0:
                switchAdBd(ctrl & 0x0F);
                if (band_low != 0) rixC0Pro(ctrl & 0x0F, band_low);
                break;
            default:
                band = ((ctrl << 8) + band_low) & 0xffff;
                break;
            }
            if (band != 0) return band;
        }
        musicCtrl();
        I = mus_block + 1;
        band = 0;
        music_on = 1;
        return 0;
    }

    private void rixGetIns() {
        if (ins_block + (band_low << 6) + 2 * 28 >= length) return;

        long baddr = ins_block + ((long) band_low << 6);

        for (int i = 0; i < 28; i++) {
            insbuf[i] = (rb(baddr + i * 2 + 1) << 8) + rb(baddr + i * 2);
        }
    }

    private void rix90Pro(int ctrlL) {
        if (ctrlL >= 11) return; // modify[] has size 28
        if (rhythm == 0 || ctrlL < 6) {
            insToReg(modify[ctrlL * 2], insbuf, 0, insbuf[26]);
            insToReg(modify[ctrlL * 2 + 1], insbuf, 13, insbuf[27]);
        } else if (ctrlL > 6) {
            insToReg(modify[ctrlL * 2 + 6], insbuf, 0, insbuf[26]);
        } else { // same effect as 1st case, no need to handle separately
            insToReg(12, insbuf, 0, insbuf[26]);
            insToReg(15, insbuf, 13, insbuf[27]);
        }
    }

    private void rixA0Pro(int ctrlL, int index) {
        if (rhythm == 0 || ctrlL <= 6) {
            prepareA0b0(ctrlL, Math.min(index, 0x3FFF));
            adA0b0lReg(ctrlL, a0b0_data3[ctrlL], a0b0_data4[ctrlL]);
        }
    }

    /** important! 16-bit assembly transcription; s16/u16 semantics matter */
    private void prepareA0b0(int index, int v) {
        if (index >= 11) return;
        int high, low;
        long res;
        int res1 = (v - 0x2000) * 0x19;
        if (res1 == 0xff) return; // impossible
        low = res1 / 0x2000;
        if (low < 0) {
            low = 0x18 - low;
            high = (short) low < 0 ? 0xFFFF : 0;
            res = ((long) high << 16) + (short) low; // res += low sign-extends
            res &= 0xffffffffL;
            low = (short) res / (short) 0xFFE7; // s16 division by -25
            a0b0_data2[index] = low;
            low = (short) res;
            res = (low - 0x18) & 0xffffffffL;
            high = (short) res % 0x19;
            low = (short) res / 0x19;
            if (high != 0) {
                low = 0x19;
                low = low - high;
            }
        } else {
            res = high = low;
            low = (short) res / 0x19;
            a0b0_data2[index] = low;
            res = high & 0xffffffffL;
            low = (short) res % 0x19;
        }
        low = (short) ((short) low * 0x18);
        displace[index] = low & 0xffff;
    }

    private void adA0b0lReg(int index, int p2, int p3) {
        if (index >= 11) return;
        int data;
        int i = (p2 + a0b0_data2[index]) & 0xffff;
        a0b0_data4[index] = p3;
        a0b0_data3[index] = p2;
        i = (short) i <= 0x5F ? i : 0x5F;
        i = (short) i >= 0 ? i : 0;
        data = f_buffer[addrs_head[i] + displace[index] / 2]; // sum <= 11+24*24/2 < 300
        adBop(0xA0 + index, data);
        data = a0b0_data5[i] * 4 + (p3 < 1 ? 0 : 0x20) + ((data >> 8) & 3);
        adBop(0xB0 + index, data);
    }

    private void adA0b0lReg_(int index, int p2, int p3) {
        a0b0_data4[index] = p3;
        a0b0_data3[index] = p2;
    }

    private void rixB0Pro(int ctrlL, int index) {
        if (ctrlL >= 11) return;
        int temp;
        if (rhythm == 0 || ctrlL < 6) {
            temp = modify[ctrlL * 2 + 1];
        } else {
            temp = ctrlL > 6 ? ctrlL * 2 : ctrlL * 2 + 1;
            temp = modify[temp + 6];
        }
        for40reg[temp] = Math.min(index, 0x7F);
        ad40Reg(temp);
    }

    private void rixC0Pro(int ctrlL, int index) {
        int i = index >= 12 ? index - 12 : 0;
        if (ctrlL < 6 || rhythm == 0) {
            adA0b0lReg(ctrlL, i, 1);
        } else {
            if (ctrlL != 6) {
                if (ctrlL == 8) {
                    adA0b0lReg(ctrlL, i, 0);
                    adA0b0lReg(7, i + 7, 0);
                }
            } else {
                adA0b0lReg(ctrlL, i, 0);
            }
            bd_modify |= bd_reg_data[ctrlL];
            adBdReg();
        }
    }

    private void switchAdBd(int index) {
        if (rhythm == 0 || index < 6) {
            adA0b0lReg(index, a0b0_data3[index], 0);
        } else {
            bd_modify &= ~bd_reg_data[index];
            adBdReg();
        }
    }

    private void insToReg(int index, int[] insb, int insbOff, int value) {
        for (int i = 0; i < 13; i++) {
            reg_bufs[index][i] = insb[insbOff + i] & 0xff;
        }
        reg_bufs[index][13] = value & 3;
        adBdReg();
        adBop(8, 0); // ad_08_reg
        ad40Reg(index);
        adC0Reg(index);
        ad60Reg(index);
        ad80Reg(index);
        ad20Reg(index);
        adE0Reg(index);
    }

    private void adE0Reg(int index) {
        int data = e0_reg_flag == 0 ? 0 : (reg_bufs[index][13] & 3);
        adBop(0xE0 + reg_data[index], data);
    }

    private void ad20Reg(int index) {
        int data = reg_bufs[index][9] < 1 ? 0 : 0x80;
        data += reg_bufs[index][10] < 1 ? 0 : 0x40;
        data += reg_bufs[index][5] < 1 ? 0 : 0x20;
        data += reg_bufs[index][11] < 1 ? 0 : 0x10;
        data += reg_bufs[index][1] & 0x0F;
        adBop(0x20 + reg_data[index], data);
    }

    private void ad80Reg(int index) {
        int data = reg_bufs[index][7] & 0x0F;
        int temp = reg_bufs[index][4];
        data |= temp << 4;
        adBop(0x80 + reg_data[index], data);
    }

    private void ad60Reg(int index) {
        int data = reg_bufs[index][6] & 0x0F;
        int temp = reg_bufs[index][3];
        data |= temp << 4;
        adBop(0x60 + reg_data[index], data);
    }

    private void adC0Reg(int index) {
        int data = reg_bufs[index][2];
        if (adflag[index] == 1) return;
        data *= 2;
        data |= reg_bufs[index][12] < 1 ? 1 : 0;
        adBop(0xC0 + ad_C0_offs[index], data);
    }

    private void ad40Reg(int index) {
        long res;
        int data, temp = reg_bufs[index][0];
        data = 0x3F - (0x3F & reg_bufs[index][8]);
        data = (data * for40reg[index]) & 0xffff;
        data = (data * 2) & 0xffff;
        data = (data + 0x7F) & 0xffff;
        res = data;
        data = (int) (res / 0xFE);
        data = (data - 0x3F) & 0xffff;
        data = (-data) & 0xffff;
        data |= temp << 6;
        adBop(0x40 + reg_data[index], data);
    }

    private void adBdReg() {
        int data = rhythm < 1 ? 0 : 0x20;
        data |= bd_modify;
        adBop(0xBD, data);
    }

    private void musicCtrl() {
        for (int i = 0; i < 11; i++) {
            switchAdBd(i);
        }
    }

    // ---- byte helpers

    /** little-endian unsigned 16-bit */
    private static int u16b(byte[] b, int off) {
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8);
    }

    /** little-endian unsigned 32-bit */
    private static long u32(byte[] b, int off) {
        return (b[off] & 0xffL) | ((b[off + 1] & 0xffL) << 8) | ((b[off + 2] & 0xffL) << 16) | ((b[off + 3] & 0xffL) << 24);
    }
}
