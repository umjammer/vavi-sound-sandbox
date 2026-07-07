/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2008 Simon Peter, <dn.tlp@gmx.net>, et al.
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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;

import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

import static java.lang.System.getLogger;


/**
 * D00 Player (EdLib packed).
 *
 * @author Simon Peter <dn.tlp@gmx.net>
 */
public class D00Player extends Opl3Player {

    private static final Logger logger = getLogger(D00Player.class.getName());

    /** "JCH\x26\x02\x66" */
    private static final byte[] ID = {'J', 'C', 'H', 0x26, 0x02, 0x66};

    /** D00 note table */
    private static final int[] notetable = {
        340, 363, 385, 408, 432, 458, 485, 514, 544, 577, 611, 647
    };

    /** the 9 operators as expected by the OPL */
    private static final int[] op_table = {
        0x00, 0x01, 0x02, 0x08, 0x09, 0x0a, 0x10, 0x11, 0x12
    };

    /** per-channel state (unsigned unless commented) */
    private static class Channel {
        int orderOff;   // pointer -> offset into filedata (-1 == disabled)
        int ordpos, pattpos, del, speed, rhcnt, key, freq, inst, spfx, ispfx, irhcnt;
        int transpose, slide, slideval, vibspeed; // signed
        int seqend, vol, vibdepth, fxdel, modvol, cvol, levpuls, frameskip, nextnote, note, ilevpuls, trigger, fxflag;
    }

    private final Channel[] channel = new Channel[9];

    private byte[] filedata;
    private int filesize;
    private int version;
    private int speed;
    private int subsongs;
    private int cursubsong;
    private int songend;

    // resolved absolute offsets into filedata (-1 == null)
    private int tpoin;
    private int instOff;
    private int seqptrOff;
    private int spfxOff;
    private int levpulsOff;

    public D00Player() {
        for (int i = 0; i < channel.length; i++) {
            channel[i] = new Channel();
        }
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("EdLib packed", "d00");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("D00");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(6);
            byte[] id = new byte[6];
            int n = bitStream.read(id);
            for (int i = 0; i < 6; i++) {
                if (n < 6 || id[i] != ID[i]) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return false;
        } finally {
            try {
                bitStream.reset();
            } catch (IOException e) {
                logger.log(Level.DEBUG, e.toString());
            }
        }
    }

    @Override
    public int getTotalMilliseconds() {
        return 0;
    }

    // ---- filedata accessors ----

    private int u8(int off) {
        return off >= 0 && off < filesize ? filedata[off] & 0xff : 0;
    }

    private int s8(int off) {
        return (byte) u8(off);
    }

    private int u16(int off) {
        return off >= 0 && off + 1 < filesize ? (filedata[off] & 0xff) | ((filedata[off + 1] & 0xff) << 8) : 0;
    }

    /** INDEX_OK: element {@code idx} of the array at {@code baseOff} (each {@code elem} bytes) fits in the file */
    private boolean indexOk(int baseOff, int idx, int elem) {
        return baseOff >= 0 && baseOff + (idx + 1) * elem <= filesize;
    }

    // instrument: Sinsts { data[11], tunelev, timer, sr, dummy[2] } = 16 bytes
    private boolean instOk(int i) { return indexOk(instOff, i, 16); }
    private int instData(int i, int k) { return u8(instOff + i * 16 + k); }
    private int instTunelev(int i) { return u8(instOff + i * 16 + 11); }
    private int instTimer(int i) { return u8(instOff + i * 16 + 12); }
    private int instSr(int i) { return u8(instOff + i * 16 + 13); }

    // spfx: Sspfx { instnr(u16), halfnote(s8), modlev(u8), modlevadd(s8), duration(u8), ptr(u16) } = 8 bytes
    private boolean spfxOk(int i) { return indexOk(spfxOff, i, 8); }
    private int spfxInstnr(int i) { return u16(spfxOff + i * 8); }
    private int spfxHalfnote(int i) { return s8(spfxOff + i * 8 + 2); }
    private int spfxModlev(int i) { return u8(spfxOff + i * 8 + 3); }
    private int spfxModlevadd(int i) { return s8(spfxOff + i * 8 + 4); }
    private int spfxDuration(int i) { return u8(spfxOff + i * 8 + 5); }
    private int spfxPtr(int i) { return u16(spfxOff + i * 8 + 6); }

    // levpuls: Slevpuls { level(u8), voladd(s8), duration(u8), ptr(u8) } = 4 bytes
    private boolean levpulsOk(int i) { return indexOk(levpulsOff, i, 4); }
    private int levpulsLevel(int i) { return u8(levpulsOff + i * 4); }
    private int levpulsVoladd(int i) { return s8(levpulsOff + i * 4 + 1); }
    private int levpulsDuration(int i) { return u8(levpulsOff + i * 4 + 2); }
    private int levpulsPtr(int i) { return u8(levpulsOff + i * 4 + 3); }

    // seqptr: array of u16
    private boolean seqptrOk(int i) { return indexOk(seqptrOff, i, 2); }
    private int seqptr(int i) { return u16(seqptrOff + i * 2); }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] raw = is.readAllBytes();

        boolean ver1 = false;
        int headerstart = 0;

        // file validation section
        // Check for reheadered old-style song
        boolean jch = raw.length >= 8 && matchId(raw, 0);
        if (jch && (raw[7] & 0x80) != 0) {
            // reheadered old song: old header begins 0x6b into the file
            headerstart = 0x6b;
            ver1 = true;
        } else if (!jch || raw[6] != 0 /* type */ || raw[9] == 0 /* subsongs */ ||
                raw[10] != 0 /* soundcard */ || (raw[7] & 0xff) < 2 || (raw[7] & 0xff) > 4) {
            // version 0 or 1 header
            ver1 = true;
        }

        // load section: filedata is read from headerstart (0-padded past EOF), filesize is the full size
        filesize = raw.length;
        filedata = new byte[filesize + 1];
        System.arraycopy(raw, headerstart, filedata, 0, filesize - headerstart);

        if (!ver1) { // version 2 and above (d00header at offset 0)
            version = u8(7);
            int infoptr = u16(113), instptr = u16(111), seqp = u16(109);
            if (filesize < 119 || filesize < infoptr || filesize < instptr || filesize < seqp) {
                throw new IllegalArgumentException("d00: bad header pointers");
            }
            instOff = instptr;
            seqptrOff = seqp;
            tpoin = u16(107);
            subsongs = u8(9);
            speed = u8(8);
        } else { // version 0 and 1 (d00header1 at offset 0)
            version = u8(0);
            int infoptr = u16(9), instptr = u16(7), seqp = u16(5);
            if (filesize < 15 || filesize <= infoptr || filesize <= instptr || filesize <= seqp) {
                throw new IllegalArgumentException("d00: bad header pointers");
            }
            instOff = instptr;
            seqptrOff = seqp;
            tpoin = u16(3);
            subsongs = u8(2);
            speed = u8(1);
        }

        spfxOff = -1;
        levpulsOff = -1;
        switch (version) {
        case 0:
            speed = 70; // v0 files default to 70Hz
            break;
        case 1:
            if (filesize <= u16(11)) throw new IllegalArgumentException("d00: bad lpulptr"); // header1.lpulptr
            levpulsOff = u16(11);
            break;
        case 2:
            if (filesize <= u16(115)) throw new IllegalArgumentException("d00: bad spfxptr"); // header.spfxptr
            levpulsOff = u16(115);
            break;
        case 3:
            break;
        case 4:
            if (filesize <= u16(115)) throw new IllegalArgumentException("d00: bad spfxptr");
            spfxOff = u16(115);
            break;
        default:
            throw new IllegalArgumentException("d00: unsupported version " + version);
        }

logger.log(Level.DEBUG, "version: " + version + ", subsongs: " + subsongs + ", speed: " + speed);

        rewind(0);
    }

    private static boolean matchId(byte[] b, int off) {
        for (int i = 0; i < ID.length; i++) {
            if (b[off + i] != ID[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean update() {
        // effect handling (timer dependent)
        for (int c = 0; c < 9; c++) {
            Channel ch = channel[c];
            ch.slideval = (short) (ch.slideval + ch.slide);
            setfreq(c); // sliding
            vibrato(c); // vibrato

            if (ch.spfx != 0xffff) { // SpFX
                boolean doVol = true;
                if (ch.fxdel != 0) {
                    ch.fxdel--;
                } else {
                    ch.spfx = spfxPtr(ch.spfx);
                    if (ch.spfx == 0xffff || !spfxOk(ch.spfx)) {
                        ch.spfx = 0xffff;
                        doVol = false;
                    } else {
                        ch.fxdel = spfxDuration(ch.spfx);
                        ch.inst = spfxInstnr(ch.spfx) & 0xfff;
                        if (spfxModlev(ch.spfx) != 0xff) {
                            ch.modvol = spfxModlev(ch.spfx);
                        }
                        setinst(c);
                        int note;
                        if ((spfxInstnr(ch.spfx) & 0x8000) != 0) { // locked frequency
                            note = spfxHalfnote(ch.spfx) & 0xff;
                        } else { // unlocked frequency
                            note = (spfxHalfnote(ch.spfx) + ch.note) & 0xff;
                        }
                        ch.freq = (notetable[note % 12] + ((note / 12) << 10)) & 0xffff;
                        setfreq(c);
                    }
                }
                if (doVol) {
                    ch.modvol = (ch.modvol + spfxModlevadd(ch.spfx)) & 63;
                    setvolume(c);
                }
            }

            if (ch.levpuls != 0xff) { // Levelpuls
                if (ch.frameskip != 0) {
                    ch.frameskip--;
                } else if (instOk(ch.inst)) {
                    ch.frameskip = instTimer(ch.inst);
                    if (ch.fxdel != 0) {
                        ch.fxdel--;
                    } else if (levpulsOk(ch.levpuls)) {
                        ch.levpuls = (levpulsPtr(ch.levpuls) - 1) & 0xff;
                        ch.fxdel = levpulsDuration(ch.levpuls);
                        if (levpulsLevel(ch.levpuls) != 0xff) {
                            ch.modvol = levpulsLevel(ch.levpuls);
                        }
                    }
                    ch.modvol = (ch.modvol + levpulsVoladd(ch.levpuls)) & 63;
                    setvolume(c);
                }
            }
        }

        // song handling
        for (int c = 0; c < 9; c++) {
            Channel ch = channel[c];
            if (version < 3 ? ch.del != 0 : ch.del <= 0x7f) {
                if (version == 4) { // v4: hard restart SR
                    if (instOk(ch.inst) && ch.del == instTimer(ch.inst) && ch.nextnote != 0) {
                        write(0, 0x83 + op_table[c], instSr(ch.inst));
                    }
                }
                if (version < 3) {
                    ch.del--;
                } else if (ch.speed != 0) {
                    ch.del += ch.speed;
                } else {
                    ch.seqend = 1;
                    continue;
                }
            } else {
                if (ch.speed != 0) {
                    if (version < 3) {
                        ch.del = ch.speed;
                    } else {
                        ch.del &= 0x7f;
                        ch.del += ch.speed;
                    }
                } else {
                    ch.seqend = 1;
                    continue;
                }
                if (ch.rhcnt != 0) { // process pending REST/HOLD events
                    ch.rhcnt--;
                    continue;
                }
                readorder(c);
            }
        }

        int trackend = 0;
        for (int c = 0; c < 9; c++) {
            if (channel[c].seqend != 0) {
                trackend++;
            }
        }
        if (trackend == 9) {
            songend = 1;
        }

        return songend == 0;
    }

    /** process arrangement (orderlist) and sequence (pattern) for one channel */
    private void readorder(int c) {
        Channel ch = channel[c];
        int patt;

        while (true) { // readorder:
            if (!indexOk(ch.orderOff, ch.ordpos, 2)) {
                ch.seqend = 1;
                return;
            }
            int ord = u16(ch.orderOff + ch.ordpos * 2);
            if (ord == 0xfffe) { // end of arrangement stream
                ch.seqend = 1;
                return;
            } else if (ord == 0xffff) { // jump to order
                ch.seqend = 1;
                if (!indexOk(ch.orderOff, ch.ordpos + 1, 2)) {
                    return;
                }
                ch.ordpos = u16(ch.orderOff + (ch.ordpos + 1) * 2);
                continue; // goto readorder
            } else {
                if (ord >= 0x9000) { // set speed
                    ch.speed = ord & 0xff;
                    if (ch.ordpos > 0) {
                        ord = u16(ch.orderOff + (ch.ordpos - 1) * 2);
                    } else {
                        ord = 0;
                    }
                    ch.ordpos++;
                } else if (ord >= 0x8000) { // transpose track
                    ch.transpose = (byte) (ord & 0xff);
                    if ((ord & 0x100) != 0) {
                        ch.transpose = (byte) -ch.transpose;
                    }
                    if (!indexOk(ch.orderOff, ch.ordpos + 1, 2)) {
                        ch.seqend = 1;
                        return;
                    }
                    ord = u16(ch.orderOff + (++ch.ordpos) * 2);
                }
                if (!seqptrOk(ord) || seqptr(ord) + 2 > filesize) {
                    ch.seqend = 1;
                    return;
                }
                patt = seqptr(ord);
            }

            ch.fxflag = 0;
            while (true) { // readseq:
                if (version == 0) { // v0: always initialize rhcnt
                    ch.rhcnt = ch.irhcnt;
                }
                int pattpos = indexOk(patt, ch.pattpos, 2) ? u16(patt + ch.pattpos * 2) : 0xffff;
                if (pattpos == 0xffff) { // pattern ended?
                    ch.pattpos = 0;
                    ch.ordpos++;
                    break; // goto readorder
                }
                int cnt = (pattpos >> 8) & 0xff;
                int note = pattpos & 0xff;
                int fx = pattpos >> 12;
                int fxop = pattpos & 0x0fff;
                ch.pattpos++;
                pattpos = indexOk(patt, ch.pattpos, 2) ? u16(patt + ch.pattpos * 2) : 0;
                ch.nextnote = (pattpos & 0xff) & 0x7f;

                if (version != 0 ? cnt < 0x40 : fx == 0) { // note event
                    switch (note) {
                    case 0: // REST event
                    case 0x80:
                        if (note == 0 || version != 0) {
                            ch.key = 0;
                            setfreq(c);
                        }
                        // fall through...
                    case 0x7e: // HOLD event
                        if (version != 0) {
                            ch.rhcnt = cnt;
                        }
                        ch.nextnote = 0;
                        break;
                    default: // play note
                        // restart fx
                        if ((ch.fxflag & 1) == 0) {
                            ch.vibdepth = 0;
                        }
                        if ((ch.fxflag & 2) == 0) {
                            ch.slideval = ch.slide = 0;
                        }

                        if (version != 0) { // note handling for v1 and above
                            if (note > 0x80) { // locked note (no channel transpose)
                                note -= 0x80;
                            } else { // unlocked note
                                note = (note + ch.transpose) & 0xff;
                            }
                            ch.note = note; // remember note for SpFX

                            if (ch.ispfx != 0xffff && cnt < 0x20 && spfxOk(ch.ispfx)) { // reset SpFX
                                ch.spfx = ch.ispfx;
                                if ((spfxInstnr(ch.spfx) & 0x8000) != 0) { // locked frequency
                                    note = spfxHalfnote(ch.spfx) & 0xff;
                                } else { // unlocked frequency
                                    note = (note + spfxHalfnote(ch.spfx)) & 0xff;
                                }
                                ch.inst = spfxInstnr(ch.spfx) & 0xfff;
                                ch.fxdel = spfxDuration(ch.spfx);
                                if (spfxModlev(ch.spfx) != 0xff) {
                                    ch.modvol = spfxModlev(ch.spfx);
                                } else {
                                    ch.modvol = instData(ch.inst, 7) & 63;
                                }
                            }

                            if (ch.ilevpuls != 0xff && cnt < 0x20 && levpulsOk(ch.ilevpuls) && instOk(ch.inst)) { // reset LevelPuls
                                ch.levpuls = ch.ilevpuls;
                                ch.fxdel = levpulsDuration(ch.levpuls);
                                ch.frameskip = instTimer(ch.inst);
                                if (levpulsLevel(ch.levpuls) != 0xff) {
                                    ch.modvol = levpulsLevel(ch.levpuls);
                                } else {
                                    ch.modvol = instData(ch.inst, 7) & 63;
                                }
                            }

                            ch.freq = (notetable[note % 12] + ((note / 12) << 10)) & 0xffff;
                            if (cnt < 0x20) { // normal note
                                playnote(c);
                            } else { // tienote
                                setfreq(c);
                                cnt -= 0x20; // make count proper
                            }
                            ch.rhcnt = cnt;
                        } else { // note handling for v0
                            if (cnt < 2) { // unlocked note
                                note = (note + ch.transpose) & 0xff;
                            }
                            ch.note = note;

                            ch.freq = (notetable[note % 12] + ((note / 12) << 10)) & 0xffff;
                            if (cnt == 1) { // tienote
                                setfreq(c);
                            } else { // normal note
                                playnote(c);
                            }
                        }
                        break;
                    }
                    return; // event is complete
                } else { // effect event
                    switch (fx) {
                    case 6: // Cut/Stop Voice
                        int buf = ch.inst;
                        ch.inst = 0;
                        playnote(c);
                        ch.inst = buf;
                        ch.rhcnt = fxop;
                        return; // no note follows this event
                    case 7: // Vibrato
                        ch.vibspeed = fxop & 0xff;
                        ch.vibdepth = fxop >> 8;
                        ch.trigger = fxop >> 9;
                        ch.fxflag |= 1;
                        break;
                    case 8: // v0: Duration
                        if (version == 0) {
                            ch.irhcnt = fxop;
                        }
                        break;
                    case 9: // New Level
                        ch.vol = fxop & 63;
                        if (ch.vol + ch.cvol < 63) { // apply channel volume
                            ch.vol += ch.cvol;
                        } else {
                            ch.vol = 63;
                        }
                        setvolume(c);
                        break;
                    case 0xb: // v4: Set SpFX
                        if (version == 4) {
                            ch.ispfx = fxop;
                        }
                        break;
                    case 0xc: // Set Instrument
                        ch.ispfx = 0xffff;
                        ch.spfx = 0xffff;
                        ch.inst = fxop;
                        if (!instOk(fxop)) {
                            ch.modvol = 0;
                            ch.levpuls = ch.ilevpuls = 0xff;
                            break;
                        }
                        ch.modvol = instData(fxop, 7) & 63;
                        if (version < 3 && version != 0 && instTunelev(fxop) != 0) { // Set LevelPuls
                            ch.ilevpuls = instTunelev(fxop) - 1;
                        } else {
                            ch.ilevpuls = 0xff;
                            ch.levpuls = 0xff;
                        }
                        break;
                    case 0xd: // Slide up
                        ch.slide = fxop;
                        ch.fxflag |= 2;
                        break;
                    case 0xe: // Slide down
                        ch.slide = -fxop;
                        ch.fxflag |= 2;
                        break;
                    }
                    // goto readseq: event is incomplete, note follows
                }
            }
        }
    }

    @Override
    public void rewind(int subSong) {
        if (subSong < 0) {
            subSong = cursubsong;
        }

        // Stpoin { unsigned short ptr[9]; unsigned char volume[9], dummy[5]; } = 32 bytes
        int dataofs = subSong * 32 + tpoin;
        boolean valid = subSong < getSubSongs() && dataofs + 32 <= filesize;

        for (int i = 0; i < 9; i++) {
            channel[i] = new Channel();
            Channel ch = channel[i];

            int ptr = valid ? u16(dataofs + i * 2) : 0;
            if (ptr != 0 && ptr + 4 <= filesize) { // track enabled
                ch.speed = u16(ptr);
                ch.orderOff = ptr + 2;
            } else { // track disabled
                ch.speed = 0;
                ch.orderOff = -1;
            }
            ch.ispfx = 0xffff;
            ch.spfx = 0xffff;
            ch.ilevpuls = 0xff;
            ch.levpuls = 0xff;
            ch.cvol = valid ? u8(dataofs + 18 + i) & 0x7f : 0; // our player may safely ignore bit 7
            ch.vol = ch.cvol; // initialize volume
        }
        songend = 0;
        for (int i = 0; i < 256; i++) {
            write(0, i, 0);
        }
        write(0, 1, 32); // reset OPL chip -> OPL2 mode
        cursubsong = Math.min(subSong, 0xff);
    }

    private int getSubSongs() {
        return subsongs;
    }

    @Override
    public float getRefresh() {
        return speed;
    }

    // ---- private methods ----

    private void setvolume(int chan) {
        int op = op_table[chan];
        int insnr = channel[chan].inst;
        if (!instOk(insnr)) {
            return;
        }

        write(0, 0x43 + op, (int) (63 - ((63 - (instData(insnr, 2) & 63)) / 63.0) * (63 - channel[chan].vol)) +
                (instData(insnr, 2) & 192));
        if ((instData(insnr, 10) & 1) != 0) {
            write(0, 0x40 + op, (int) (63 - ((63 - channel[chan].modvol) / 63.0) * (63 - channel[chan].vol)) +
                    (instData(insnr, 7) & 192));
        } else {
            write(0, 0x40 + op, channel[chan].modvol + (instData(insnr, 7) & 192));
        }
    }

    private void setfreq(int chan) {
        int freq = channel[chan].freq;

        if (version == 4 && instOk(channel[chan].inst)) { // v4: apply instrument finetune
            freq += instTunelev(channel[chan].inst);
        }

        freq = (freq + channel[chan].slideval) & 0xffff;
        write(0, 0xa0 + chan, freq & 255);
        if (channel[chan].key != 0) {
            write(0, 0xb0 + chan, ((freq >> 8) & 31) | 32);
        } else {
            write(0, 0xb0 + chan, (freq >> 8) & 31);
        }
    }

    private void setinst(int chan) {
        int op = op_table[chan];
        int insnr = channel[chan].inst;
        if (!instOk(insnr)) {
            return;
        }

        // set instrument data
        write(0, 0x63 + op, instData(insnr, 0));
        write(0, 0x83 + op, instData(insnr, 1));
        write(0, 0x23 + op, instData(insnr, 3));
        write(0, 0xe3 + op, instData(insnr, 4));
        write(0, 0x60 + op, instData(insnr, 5));
        write(0, 0x80 + op, instData(insnr, 6));
        write(0, 0x20 + op, instData(insnr, 8));
        write(0, 0xe0 + op, instData(insnr, 9));
        if (version != 0) {
            write(0, 0xc0 + chan, instData(insnr, 10));
        } else {
            write(0, 0xc0 + chan, (instData(insnr, 10) << 1) + (instTunelev(insnr) & 1));
        }
    }

    private void playnote(int chan) {
        // set misc vars & play
        write(0, 0xb0 + chan, 0); // stop old note
        setinst(chan);
        channel[chan].key = 1;
        setfreq(chan);
        setvolume(chan);
    }

    private void vibrato(int chan) {
        Channel ch = channel[chan];
        if (ch.vibdepth == 0) {
            return;
        }

        if (ch.trigger != 0) {
            ch.trigger--;
        } else {
            ch.trigger = ch.vibdepth;
            ch.vibspeed = -ch.vibspeed;
        }
        ch.freq = (ch.freq + ch.vibspeed) & 0xffff;
        setfreq(chan);
    }
}
