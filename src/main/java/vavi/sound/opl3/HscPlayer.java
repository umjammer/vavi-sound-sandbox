/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2004 Simon Peter, <dn.tlp@gmx.net>, et al.
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
 * HSC Adlib Composer / HSC-Tracker Player.
 *
 * @author Simon Peter <dn.tlp@gmx.net>
 */
public class HscPlayer extends Opl3Player {

    private static final Logger logger = getLogger(HscPlayer.class.getName());

    /** standard adlib note table */
    private static final int[] note_table = {
        363, 385, 408, 432, 458, 485, 514, 544, 577, 611, 647, 686
    };

    /** the 9 operators as expected by the OPL */
    private static final int[] op_table = {
        0x00, 0x01, 0x02, 0x08, 0x09, 0x0a, 0x10, 0x11, 0x12
    };

    /** header size: 128 * 12 instrument bytes + 51 tracklist bytes */
    private static final int HEADER_SIZE = 1587;
    /** size of a single pattern: 64 rows * 9 channels * 2 bytes */
    private static final int PATTERN_SIZE = 1152;
    /** +1 is for some files that have a trailing 0x00 on the end */
    private static final int MAX_SIZE = 59187 + 1;
    /** no 0x00 byte here as this is the smallest possible size */
    private static final int MIN_SIZE = HEADER_SIZE + PATTERN_SIZE;

    /** HSC channel data */
    private static class HscChannel {
        /** current instrument */
        int inst;
        /** used for manual slide-effects */
        int slide;
        /** actual replaying frequency */
        int freq;
    }

    /** player channel-info */
    private final HscChannel[] channel = new HscChannel[9];
    /** instrument data */
    protected final int[][] instr = new int[128][12];
    /** song-arrangement (MPU-401 Trakker enhanced) */
    protected final int[] song = new int[0x80];
    /** pattern data ([pattern][row * 9 + channel][0: note, 1: effect]) */
    protected final int[][][] patterns = new int[50][64 * 9][2];
    /** various bytes & flags */
    private int pattpos, songpos, pattbreak, songend, mode6, bd, fadein;
    private int speed, del;
    /** adlib frequency registers */
    private final int[] adl_freq = new int[9];
    /** flag: MPU-401 Trakker mode on/off */
    protected int mtkmode = 0;

    public HscPlayer() {
        for (int i = 0; i < channel.length; i++) {
            channel[i] = new HscChannel();
        }
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("HSC Adlib Composer / HSC-Tracker", "hsc");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("HSC");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(MAX_SIZE + 1);

            // file validation section: HSC has no signature, so the only
            // check we can do is a file-size sanity test (as adplug does).
            int size = 0;
            byte[] buf = new byte[8192];
            int r;
            while (size <= MAX_SIZE && (r = bitStream.read(buf)) != -1) {
                size += r;
            }

            return size <= MAX_SIZE && size >= MIN_SIZE;
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

    @Override
    public void load(InputStream is) throws IOException {
        byte[] data = is.readAllBytes();

        int total_patterns_in_hsc = (data.length - HEADER_SIZE) / PATTERN_SIZE;

        // load section
        for (int i = 0; i < 128 * 12; i++) { // load instruments
            instr[i / 12][i % 12] = data[i] & 0xff;
        }
        for (int i = 0; i < 128; i++) { // correct instruments
            instr[i][2] ^= (instr[i][2] & 0x40) << 1;
            instr[i][3] ^= (instr[i][3] & 0x40) << 1;
            instr[i][11] >>= 4; // slide
        }
        for (int i = 0; i < 51; i++) { // load tracklist
            song[i] = data[128 * 12 + i] & 0xff;
            // if out of range, song ends here
            if (((song[i] & 0x7f) > 0x31) || ((song[i] & 0x7f) >= total_patterns_in_hsc)) {
                song[i] = 0xff;
            }
        }
        // load patterns (adplug reads 50 * 64 * 9 bytes, padding past EOF with 0)
        for (int i = 0; i < 50 * 64 * 9; i++) {
            int off = HEADER_SIZE + i;
            int b = off < data.length ? data[off] & 0xff : 0;
            int idx = i >> 1; // hscnote index
            patterns[idx / (64 * 9)][idx % (64 * 9)][i & 1] = b;
        }

logger.log(Level.DEBUG, "patterns: " + total_patterns_in_hsc);

        rewind(0); // rewind module
    }

    @Override
    public boolean update() {
        // general vars
        int chan, pattnr, note, effect, eff_op, inst, vol, Okt, db;
        int Fnr;
        int pattoff;

        del--; // player speed handling
        if (del != 0) {
            return songend == 0; // nothing done
        }

        if (fadein != 0) { // fade-in handling
            fadein--;
        }

        pattnr = song[songpos];
        // 0xff indicates song end, but this prevents a crash for some songs that
        // use other weird values, like 0xbf
        if (pattnr >= 0xb2) { // arrangement handling
            songend = 1; // set end-flag
            songpos = 0;
            pattnr = song[songpos];
        } else if ((pattnr & 128) != 0 && (pattnr <= 0xb1)) { // goto pattern "nr"
            songpos = song[songpos] & 127;
            pattpos = 0;
            pattnr = song[songpos];
            songend = 1;
        }

        // avoid reading outside valid "pattern" data, in case of double jump in the
        // song[] data, invalid song termination in song[0] etc.
        if (pattnr < 50) {
            pattoff = pattpos * 9;
            for (chan = 0; chan < 9; chan++) { // handle all channels
                note = patterns[pattnr][pattoff][0];
                effect = patterns[pattnr][pattoff][1];
                pattoff++;

                if ((note & 128) != 0) { // set instrument
                    setinstr(chan, effect);
                    continue;
                }
                eff_op = effect & 0x0f;
                inst = channel[chan].inst;
                if (note != 0) {
                    channel[chan].slide = 0;
                }

                switch (effect & 0xf0) { // effect handling
                case 0: // global effect
                    /* The following fx are unimplemented on purpose:
                     * 02 - Slide Mainvolume up
                     * 03 - Slide Mainvolume down (here: fade in)
                     * 04 - Set Mainvolume to 0
                     *
                     * This is because i've never seen any HSC modules using the fx this way.
                     * All modules use the fx the way, i've implemented it.
                     */
                    switch (eff_op) {
                    case 1: pattbreak++; break; // jump to next pattern
                    case 3: fadein = 31; break; // fade in (divided by 2)
                    case 5: mode6 = 1; break; // 6 voice mode on
                    case 6: mode6 = 0; break; // 6 voice mode off
                    }
                    break;
                case 0x20:
                case 0x10: // manual slides
                    if ((effect & 0x10) != 0) {
                        channel[chan].freq = (channel[chan].freq + eff_op) & 0xffff;
                        channel[chan].slide += eff_op;
                    } else {
                        channel[chan].freq = (channel[chan].freq - eff_op) & 0xffff;
                        channel[chan].slide -= eff_op;
                    }
                    if (note == 0) {
                        setfreq(chan, channel[chan].freq);
                    }
                    break;
                case 0x50: // set percussion instrument (unimplemented)
                    break;
                case 0x60: // set feedback
                    write(0, 0xc0 + chan, (instr[channel[chan].inst][8] & 1) + (eff_op << 1));
                    break;
                case 0xa0: // set carrier volume
                    vol = eff_op << 2;
                    write(0, 0x43 + op_table[chan], vol | (instr[channel[chan].inst][2] & ~63));
                    break;
                case 0xb0: // set modulator volume
                    vol = eff_op << 2;
                    if ((instr[inst][8] & 1) != 0) {
                        write(0, 0x40 + op_table[chan], vol | (instr[channel[chan].inst][3] & ~63));
                    } else {
                        write(0, 0x40 + op_table[chan], vol | (instr[inst][3] & ~63));
                    }
                    break;
                case 0xc0: // set instrument volume
                    db = eff_op << 2;
                    write(0, 0x43 + op_table[chan], db | (instr[channel[chan].inst][2] & ~63));
                    if ((instr[inst][8] & 1) != 0) {
                        write(0, 0x40 + op_table[chan], db | (instr[channel[chan].inst][3] & ~63));
                    }
                    break;
                case 0xd0: pattbreak++; songpos = eff_op; songend = 1; break; // position jump
                case 0xf0: // set speed
                    speed = eff_op;
                    del = ++speed;
                    break;
                }

                if (fadein != 0) { // fade-in volume setting
                    setvolume(chan, fadein * 2, fadein * 2);
                }

                if (note == 0) { // note handling
                    continue;
                }
                note--;

                if ((note == 0x7f - 1) || (((note / 12) & ~7) != 0)) { // pause (7fh)
                    adl_freq[chan] &= ~32;
                    write(0, 0xb0 + chan, adl_freq[chan]);
                    continue;
                }

                // play the note
                if (mtkmode != 0) { // imitate MPU-401 Trakker bug
                    note--;
                }
                Okt = ((note / 12) & 7) << 2;
                Fnr = (note_table[note % 12] + instr[inst][11] + channel[chan].slide) & 0xffff;
                channel[chan].freq = Fnr;
                if (mode6 == 0 || chan < 6) {
                    adl_freq[chan] = Okt | 32;
                } else {
                    adl_freq[chan] = Okt; // never set key for drums
                }
                write(0, 0xb0 + chan, 0);
                setfreq(chan, Fnr);
                if (mode6 != 0) {
                    switch (chan) { // play drums
                    case 6: write(0, 0xbd, bd & ~16); bd |= 48; break; // bass drum
                    case 7: write(0, 0xbd, bd & ~1); bd |= 33; break; // hihat
                    case 8: write(0, 0xbd, bd & ~2); bd |= 34; break; // cymbal
                    }
                    write(0, 0xbd, bd);
                }
            }
        }

        // skip_pattern_data:
        del = speed; // player speed-timing
        if (pattbreak != 0) { // do post-effect handling
            pattpos = 0; // pattern break!
            pattbreak = 0;
            songpos++;
            songpos %= 50;
            if (songpos == 0) {
                songend = 1;
            }
        } else {
            pattpos++;
            pattpos &= 63; // advance in pattern data
            if (pattpos == 0) {
                songpos++;
                songpos %= 50;
                if (songpos == 0) {
                    songend = 1;
                }
            }
        }
        return songend == 0; // still playing
    }

    @Override
    public void rewind(int subSong) {
        // rewind HSC player
        pattpos = 0; songpos = 0; pattbreak = 0; speed = 2;
        del = 1; songend = 0; mode6 = 0; bd = 0; fadein = 0;

        // reset OPL chip
        for (int i = 0; i < 256; i++) {
            write(0, i, 0);
        }
        write(0, 1, 32); write(0, 8, 128); write(0, 0xbd, 0);

        for (int i = 0; i < 9; i++) {
            setinstr(i, i); // init channels
        }
    }

    @Override
    public float getRefresh() {
        return 18.2f; // refresh rate is fixed at 18.2Hz
    }

    // ----

    private void setfreq(int chan, int freq) {
        adl_freq[chan] = ((adl_freq[chan] & ~3) | (freq >> 8)) & 0xff;

        write(0, 0xa0 + chan, freq & 0xff);
        write(0, 0xb0 + chan, adl_freq[chan]);
    }

    private void setvolume(int chan, int volc, int volm) {
        int[] ins = instr[channel[chan].inst];
        int op = op_table[chan];

        write(0, 0x43 + op, volc | (ins[2] & ~63));
        if ((ins[8] & 1) != 0) { // carrier
            write(0, 0x40 + op, volm | (ins[3] & ~63));
        } else {
            write(0, 0x40 + op, ins[3]); // modulator
        }
    }

    private void setinstr(int chan, int insnr) {
        int[] ins = instr[insnr];
        int op = op_table[chan];

        channel[chan].inst = insnr; // set internal instrument
        write(0, 0xb0 + chan, 0); // stop old note

        // set instrument
        write(0, 0xc0 + chan, ins[8]);
        write(0, 0x23 + op, ins[0]); // carrier
        write(0, 0x20 + op, ins[1]); // modulator
        write(0, 0x63 + op, ins[4]); // bits 0..3 = decay; 4..7 = attack
        write(0, 0x60 + op, ins[5]);
        write(0, 0x83 + op, ins[6]); // 0..3 = release; 4..7 = sustain
        write(0, 0x80 + op, ins[7]);
        write(0, 0xe3 + op, ins[9]); // bits 0..1 = Wellenform
        write(0, 0xe0 + op, ins[10]);
        setvolume(chan, ins[2] & 63, ins[3] & 63);
    }
}
