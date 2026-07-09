/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2002 Simon Peter, <dn.tlp@gmx.net>, et al.
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
 * Loudness Sound System Player.
 *
 * @author Simon Peter <dn.tlp@gmx.net>
 */
public class LdsPlayer extends Opl3Player {

    private static final Logger logger = getLogger(LdsPlayer.class.getName());

    private static class SoundBank {
        int mod_misc, mod_vol, mod_ad, mod_sr, mod_wave,
            car_misc, car_vol, car_ad, car_sr, car_wave, feedback, keyoff,
            portamento, glide, finetune, vibrato, vibdelay, mod_trem, car_trem,
            tremwait, arpeggio;
        final byte[] arp_tab = new byte[12];
        int start, size;
        int fms;
        int transp;
        int midinst, midvelo, midkey, midtrans, middum1, middum2;
    }

    private static class ChannelCheat {
        int chandelay, sound;
        int high;
    }

    private static class Channel {
        int gototune, lasttune, packpos;
        int finetune, glideto, portspeed, nextvol, volmod, volcar,
            vibwait, vibspeed, vibrate, trmstay, trmwait, trmspeed, trmrate, trmcount,
            trcwait, trcspeed, trcrate, trccount, arp_size, arp_speed, keycount,
            vibcount, arp_pos, arp_count, packwait;
        final byte[] arp_tab = new byte[12];
        final ChannelCheat chancheat = new ChannelCheat();
    }

    private static class Position {
        int patnum;
        int transpose;
    }

    private static final int[] frequency = {
        343, 344, 345, 347, 348, 349, 350, 352, 353, 354, 356, 357, 358,
        359, 361, 362, 363, 365, 366, 367, 369, 370, 371, 373, 374, 375,
        377, 378, 379, 381, 382, 384, 385, 386, 388, 389, 391, 392, 393,
        395, 396, 398, 399, 401, 402, 403, 405, 406, 408, 409, 411, 412,
        414, 415, 417, 418, 420, 421, 423, 424, 426, 427, 429, 430, 432,
        434, 435, 437, 438, 440, 442, 443, 445, 446, 448, 450, 451, 453,
        454, 456, 458, 459, 461, 463, 464, 466, 468, 469, 471, 473, 475,
        476, 478, 480, 481, 483, 485, 487, 488, 490, 492, 494, 496, 497,
        499, 501, 503, 505, 506, 508, 510, 512, 514, 516, 518, 519, 521,
        523, 525, 527, 529, 531, 533, 535, 537, 538, 540, 542, 544, 546,
        548, 550, 552, 554, 556, 558, 560, 562, 564, 566, 568, 571, 573,
        575, 577, 579, 581, 583, 585, 587, 589, 591, 594, 596, 598, 600,
        602, 604, 607, 609, 611, 613, 615, 618, 620, 622, 624, 627, 629,
        631, 633, 636, 638, 640, 643, 645, 647, 650, 652, 654, 657, 659,
        662, 664, 666, 669, 671, 674, 676, 678, 681, 683
    };

    private static final int[] vibtab = {
        0, 13, 25, 37, 50, 62, 74, 86, 98, 109, 120, 131, 142, 152, 162,
        171, 180, 189, 197, 205, 212, 219, 225, 231, 236, 240, 244, 247,
        250, 252, 254, 255, 255, 255, 254, 252, 250, 247, 244, 240, 236,
        231, 225, 219, 212, 205, 197, 189, 180, 171, 162, 152, 142, 131,
        120, 109, 98, 86, 74, 62, 50, 37, 25, 13
    };

    private static final int[] tremtab = {
        0, 0, 1, 1, 2, 4, 5, 7, 10, 12, 15, 18, 21, 25, 29, 33, 37, 42, 47,
        52, 57, 62, 67, 73, 79, 85, 90, 97, 103, 109, 115, 121, 128, 134,
        140, 146, 152, 158, 165, 170, 176, 182, 188, 193, 198, 203, 208,
        213, 218, 222, 226, 230, 234, 237, 240, 243, 245, 248, 250, 251,
        253, 254, 254, 255, 255, 255, 254, 254, 253, 251, 250, 248, 245,
        243, 240, 237, 234, 230, 226, 222, 218, 213, 208, 203, 198, 193,
        188, 182, 176, 170, 165, 158, 152, 146, 140, 134, 127, 121, 115,
        109, 103, 97, 90, 85, 79, 73, 67, 62, 57, 52, 47, 42, 37, 33, 29,
        25, 21, 18, 15, 12, 10, 7, 5, 4, 2, 1, 1, 0
    };

    private static final int[] op_table = {
        0x00, 0x01, 0x02, 0x08, 0x09, 0x0a, 0x10, 0x11, 0x12
    };

    private static final int maxsound = 0x3f;
    private static final int maxpos = 0xff;

    private SoundBank[] soundbank;
    private Channel[] channel;
    private Position[] positions;
    private int jumping, fadeonoff, allvolume, hardfade,
        tempo_now, pattplay, tempo, regbd, mode, pattlen;
    private byte[] fmchip, chandelay;
    private int posplay, jumppos, speed;
    private int[] patterns;
    private boolean playing, songlooped;
    private int numpatch, numposi, mainvolume;

    public LdsPlayer() {
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("Loudness Sound System Format", "lds");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("LDS");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            java.net.URI uri = vavi.sound.SoundUtil.getSource(bitStream);
            if (uri != null) {
                String path = uri.getPath();
                if (path != null && path.toLowerCase().endsWith(".lds")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public int getTotalMilliseconds() {
        return 0;
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();
        int off = 0;
        mode = buf[off++] & 0xff;
        if (mode > 2) {
            throw new IOException("invalid mode: " + mode);
        }
        speed = u16(buf, off); off += 2;
        tempo = buf[off++] & 0xff;
        pattlen = buf[off++] & 0xff;
        chandelay = new byte[9];
        for (int i = 0; i < 9; i++) {
            chandelay[i] = buf[off++];
        }
        regbd = buf[off++] & 0xff;

        numpatch = u16(buf, off); off += 2;
        soundbank = new SoundBank[numpatch];
        for (int i = 0; i < numpatch; i++) {
            soundbank[i] = new SoundBank();
            soundbank[i].mod_misc = buf[off++] & 0xff;
            soundbank[i].mod_vol = buf[off++] & 0xff;
            soundbank[i].mod_ad = buf[off++] & 0xff;
            soundbank[i].mod_sr = buf[off++] & 0xff;
            soundbank[i].mod_wave = buf[off++] & 0xff;
            soundbank[i].car_misc = buf[off++] & 0xff;
            soundbank[i].car_vol = buf[off++] & 0xff;
            soundbank[i].car_ad = buf[off++] & 0xff;
            soundbank[i].car_sr = buf[off++] & 0xff;
            soundbank[i].car_wave = buf[off++] & 0xff;
            soundbank[i].feedback = buf[off++] & 0xff;
            soundbank[i].keyoff = buf[off++] & 0xff;
            soundbank[i].portamento = buf[off++] & 0xff;
            soundbank[i].glide = buf[off++] & 0xff;
            soundbank[i].finetune = buf[off++] & 0xff;
            soundbank[i].vibrato = buf[off++] & 0xff;
            soundbank[i].vibdelay = buf[off++] & 0xff;
            soundbank[i].mod_trem = buf[off++] & 0xff;
            soundbank[i].car_trem = buf[off++] & 0xff;
            soundbank[i].tremwait = buf[off++] & 0xff;
            soundbank[i].arpeggio = buf[off++] & 0xff;
            for (int j = 0; j < 12; j++) {
                soundbank[i].arp_tab[j] = buf[off++];
            }
            soundbank[i].start = u16(buf, off); off += 2;
            soundbank[i].size = u16(buf, off); off += 2;
            soundbank[i].fms = buf[off++] & 0xff;
            soundbank[i].transp = u16(buf, off); off += 2;
            soundbank[i].midinst = buf[off++] & 0xff;
            soundbank[i].midvelo = buf[off++] & 0xff;
            soundbank[i].midkey = buf[off++] & 0xff;
            soundbank[i].midtrans = buf[off++] & 0xff;
            soundbank[i].middum1 = buf[off++] & 0xff;
            soundbank[i].middum2 = buf[off++] & 0xff;
        }

        numposi = u16(buf, off); off += 2;
        positions = new Position[9 * numposi];
        for (int i = 0; i < numposi; i++) {
            for (int j = 0; j < 9; j++) {
                positions[i * 9 + j] = new Position();
                positions[i * 9 + j].patnum = u16(buf, off) / 2; off += 2;
                positions[i * 9 + j].transpose = buf[off++] & 0xff;
            }
        }

        off += 2; // skip digital sounds
        int t = (buf.length - off) / 2 + 1;
        patterns = new int[t];
        int idx = 0;
        while (off < buf.length - 1) {
            patterns[idx] = (buf[off] & 0xff) | ((buf[off + 1] & 0xff) << 8);
            off += 2;
            idx++;
        }

        rewind(0);
    }

    @Override
    public boolean update() {
        int freq, octave, chan, tune, wibc, tremc, arpreg;
        boolean vbreak;
        int level;
        int i;

        if (!playing) {
            return false;
        }

        if (fadeonoff > 0) {
            if (fadeonoff <= 128) {
                if (allvolume > fadeonoff || allvolume == 0) {
                    allvolume -= fadeonoff;
                } else {
                    allvolume = 1;
                    fadeonoff = 0;
                    if (hardfade != 0) {
                        playing = false;
                        hardfade = 0;
                        for (i = 0; i < 9; i++) {
                            channel[i].keycount = 1;
                        }
                    }
                }
            } else if (((allvolume + (0x100 - fadeonoff)) & 0xff) <= mainvolume) {
                allvolume += (0x100 - fadeonoff);
            } else {
                allvolume = mainvolume;
                fadeonoff = 0;
            }
        }

        for (chan = 0; chan < 9; chan++) {
            Channel c = channel[chan];
            if (c.chancheat.chandelay > 0) {
                c.chancheat.chandelay--;
                if (c.chancheat.chandelay <= 0) {
                    playSound(c.chancheat.sound, chan, c.chancheat.high);
                }
            }
        }

        if (tempo_now <= 0) {
            vbreak = false;
            for (chan = 0; chan < 9; chan++) {
                Channel c = channel[chan];
                if (c.packwait <= 0) {
                    int patnum = positions[posplay * 9 + chan].patnum;
                    int transpose = positions[posplay * 9 + chan].transpose;

                    int comword = patterns[patnum + c.packpos];
                    int comhi = (comword >> 8) & 0xff;
                    int comlo = comword & 0xff;

                    if (comword > 0) {
                        if (comhi == 0x80) {
                            c.packwait = comlo;
                        } else if (comhi >= 0x80) {
                            switch (comhi) {
                                case 0xff:
                                    c.volcar = (((c.volcar & 0x3f) * comlo) >> 6) & 0x3f;
                                    if ((fmchip[0xc0 + chan] & 1) > 0) {
                                        c.volmod = (((c.volmod & 0x3f) * comlo) >> 6) & 0x3f;
                                    }
                                    break;
                                case 0xfe:
                                    tempo = comword & 0x3f;
                                    break;
                                case 0xfd:
                                    c.nextvol = comlo;
                                    break;
                                case 0xfc:
                                    playing = false;
                                    break;
                                case 0xfb:
                                    c.keycount = 1;
                                    break;
                                case 0xfa:
                                    vbreak = true;
                                    jumppos = (posplay + 1) & maxpos;
                                    break;
                                case 0xf9:
                                    vbreak = true;
                                    jumppos = comlo & maxpos;
                                    jumping = 1;
                                    if (jumppos < posplay) {
                                        songlooped = true;
                                    }
                                    break;
                                case 0xf8:
                                    c.lasttune = 0;
                                    break;
                                case 0xf7:
                                    c.vibwait = 0;
                                    c.vibspeed = (comlo >> 4) + 2;
                                    c.vibrate = (comlo & 15) + 1;
                                    break;
                                case 0xf6:
                                    c.glideto = comlo;
                                    break;
                                case 0xf5:
                                    c.finetune = comlo;
                                    break;
                                case 0xf4:
                                    if (hardfade <= 0) {
                                        mainvolume = comlo;
                                        allvolume = comlo;
                                        fadeonoff = 0;
                                    }
                                    break;
                                case 0xf3:
                                    if (hardfade <= 0) {
                                        fadeonoff = comlo;
                                    }
                                    break;
                                case 0xf2:
                                    c.trmstay = comlo;
                                    break;
                                default:
                                    if (comhi < 0xa0) {
                                        c.glideto = comhi & 0x1f;
                                    }
                                    break;
                            }
                        } else {
                            int sound;
                            int high;
                            byte transposeVal = (byte) (transpose & 127);
                            if ((transpose & 64) > 0) {
                                transposeVal += 64;
                                transposeVal += 64;
                            }
                            if ((transpose & 128) > 0) {
                                sound = (comlo + transposeVal) & maxsound;
                                high = comhi << 4;
                            } else {
                                sound = comlo & maxsound;
                                high = (comhi + transposeVal) << 4;
                            }

                            if (chandelay[chan] <= 0) {
                                playSound(sound, chan, high);
                            } else {
                                c.chancheat.chandelay = chandelay[chan];
                                c.chancheat.sound = sound;
                                c.chancheat.high = high;
                            }
                        }
                    }
                    c.packpos++;
                } else {
                    c.packwait--;
                }
            }

            tempo_now = tempo;
            pattplay++;
            if (vbreak) {
                pattplay = 0;
                for (i = 0; i < 9; i++) {
                    channel[i].packpos = channel[i].packwait = 0;
                }
                posplay = jumppos;
            } else if (pattplay >= pattlen) {
                pattplay = 0;
                for (i = 0; i < 9; i++) {
                    channel[i].packpos = channel[i].packwait = 0;
                }
                posplay = (posplay + 1) & maxpos;
            }
        } else {
            tempo_now--;
        }

        for (chan = 0; chan < 9; chan++) {
            Channel c = channel[chan];
            int regnum = op_table[chan];
            if (c.keycount > 0) {
                if (c.keycount == 1) {
                    setRegsAdv((byte) (0xb0 + chan), (byte) 0xdf, (byte) 0);
                }
                c.keycount--;
            }

            if (c.arp_size == 0) {
                arpreg = 0;
            } else {
                arpreg = (c.arp_tab[c.arp_pos] & 0xff) << 4;
                if (arpreg == 0x800) {
                    if (c.arp_pos > 0) {
                        c.arp_tab[0] = c.arp_tab[c.arp_pos - 1];
                    }
                    c.arp_size = 1;
                    c.arp_pos = 0;
                    arpreg = (c.arp_tab[0] & 0xff) << 4;
                }

                if (c.arp_count == c.arp_speed) {
                    c.arp_pos++;
                    if (c.arp_pos >= c.arp_size) {
                        c.arp_pos = 0;
                    }
                    c.arp_count = 0;
                } else {
                    c.arp_count++;
                }
            }

            if (c.lasttune > 0 && (c.lasttune != c.gototune)) {
                if (c.lasttune > c.gototune) {
                    if (c.lasttune - c.gototune < c.portspeed) {
                        c.lasttune = c.gototune;
                    } else {
                        c.lasttune -= c.portspeed;
                    }
                } else {
                    if (c.gototune - c.lasttune < c.portspeed) {
                        c.lasttune = c.gototune;
                    } else {
                        c.lasttune += c.portspeed;
                    }
                }

                if (arpreg >= 0x800) {
                    arpreg = c.lasttune - (arpreg ^ 0xff0) - 16;
                } else {
                    arpreg += c.lasttune;
                }

                freq = frequency[arpreg % (12 * 16)];
                octave = arpreg / (12 * 16) - 1;
                setRegs((byte) (0xa0 + chan), (byte) (freq & 0xff));
                setRegsAdv((byte) (0xb0 + chan), (byte) 0x20, (byte) (((octave << 2) + (freq >> 8)) & 0xdf));
            } else {
                if (c.vibwait <= 0) {
                    if (c.vibrate > 0) {
                        wibc = (vibtab[c.vibcount & 0x3f] & 0xff) * c.vibrate;
                        if ((c.vibcount & 0x40) == 0) {
                            tune = c.lasttune + (wibc >> 8);
                        } else {
                            tune = c.lasttune - (wibc >> 8);
                        }

                        if (arpreg >= 0x800) {
                            tune = tune - (arpreg ^ 0xff0) - 16;
                        } else {
                            tune += arpreg;
                        }

                        freq = frequency[tune % (12 * 16)];
                        octave = tune / (12 * 16) - 1;
                        setRegs((byte) (0xa0 + chan), (byte) (freq & 0xff));
                        setRegsAdv((byte) (0xb0 + chan), (byte) 0x20, (byte) (((octave << 2) + (freq >> 8)) & 0xdf));
                        c.vibcount += c.vibspeed;
                    } else if (c.arp_size != 0) {
                        if (arpreg >= 0x800) {
                            tune = c.lasttune - (arpreg ^ 0xff0) - 16;
                        } else {
                            tune = c.lasttune + arpreg;
                        }

                        freq = frequency[tune % (12 * 16)];
                        octave = tune / (12 * 16) - 1;
                        setRegs((byte) (0xa0 + chan), (byte) (freq & 0xff));
                        setRegsAdv((byte) (0xb0 + chan), (byte) 0x20, (byte) (((octave << 2) + (freq >> 8)) & 0xdf));
                    }
                } else {
                    c.vibwait--;
                    if (c.arp_size != 0) {
                        if (arpreg >= 0x800) {
                            tune = c.lasttune - (arpreg ^ 0xff0) - 16;
                        } else {
                            tune = c.lasttune + arpreg;
                        }

                        freq = frequency[tune % (12 * 16)];
                        octave = tune / (12 * 16) - 1;
                        setRegs((byte) (0xa0 + chan), (byte) (freq & 0xff));
                        setRegsAdv((byte) (0xb0 + chan), (byte) 0x20, (byte) (((octave << 2) + (freq >> 8)) & 0xdf));
                    }
                }
            }

            if (c.trmwait <= 0) {
                if (c.trmrate > 0) {
                    tremc = (tremtab[c.trmcount & 0x7f] & 0xff) * c.trmrate;
                    if ((tremc >> 8) <= (c.volmod & 0x3f)) {
                        level = (c.volmod & 0x3f) - (tremc >> 8);
                    } else {
                        level = 0;
                    }

                    if (allvolume != 0 && (fmchip[0xc0 + chan] & 1) > 0) {
                        setRegsAdv((byte) (0x40 + regnum), (byte) 0xc0, (byte) (((level * allvolume) >> 8) ^ 0x3f));
                    } else {
                        setRegsAdv((byte) (0x40 + regnum), (byte) 0xc0, (byte) (level ^ 0x3f));
                    }

                    c.trmcount += c.trmspeed;
                } else if (allvolume != 0 && (fmchip[0xc0 + chan] & 1) > 0) {
                    setRegsAdv((byte) (0x40 + regnum), (byte) 0xc0, (byte) (((((c.volmod & 0x3f) * allvolume) >> 8) ^ 0x3f) & 0x3f));
                } else {
                    setRegsAdv((byte) (0x40 + regnum), (byte) 0xc0, (byte) ((c.volmod ^ 0x3f) & 0x3f));
                }
            } else {
                c.trmwait--;
                if (allvolume != 0 && (fmchip[0xc0 + chan] & 1) > 0) {
                    setRegsAdv((byte) (0x40 + regnum), (byte) 0xc0, (byte) (((((c.volmod & 0x3f) * allvolume) >> 8) ^ 0x3f) & 0x3f));
                }
            }

            if (c.trcwait <= 0) {
                if (c.trcrate > 0) {
                    tremc = (tremtab[c.trccount & 0x7f] & 0xff) * c.trcrate;
                    if ((tremc >> 8) <= (c.volcar & 0x3f)) {
                        level = (c.volcar & 0x3f) - (tremc >> 8);
                    } else {
                        level = 0;
                    }

                    if (allvolume != 0) {
                        setRegsAdv((byte) (0x43 + regnum), (byte) 0xc0, (byte) (((level * allvolume) >> 8) ^ 0x3f));
                    } else {
                        setRegsAdv((byte) (0x43 + regnum), (byte) 0xc0, (byte) (level ^ 0x3f));
                    }

                    c.trccount += c.trcspeed;
                } else if (allvolume != 0) {
                    setRegsAdv((byte) (0x43 + regnum), (byte) 0xc0, (byte) (((((c.volcar & 0x3f) * allvolume) >> 8) ^ 0x3f) & 0x3f));
                } else {
                    setRegsAdv((byte) (0x43 + regnum), (byte) 0xc0, (byte) ((c.volcar ^ 0x3f) & 0x3f));
                }
            } else {
                c.trcwait--;
                if (allvolume != 0) {
                    setRegsAdv((byte) (0x43 + regnum), (byte) 0xc0, (byte) (((((c.volcar & 0x3f) * allvolume) >> 8) ^ 0x3f) & 0x3f));
                }
            }
        }

        return (!playing || songlooped) ? false : true;
    }

    private void playSound(int inst_number, int channel_number, int tunehigh) {
        Channel c = channel[channel_number];
        SoundBank sb = soundbank[inst_number];
        int regnum = op_table[channel_number];
        int volcalc;

        tunehigh += ((sb.finetune + c.finetune + 0x80) & 0xff) - 0x80;

        if (sb.arpeggio <= 0) {
            int arpcalc = (sb.arp_tab[0] & 0xff) << 4;
            if (arpcalc > 0x800) {
                tunehigh = tunehigh - (arpcalc ^ 0xff0) - 16;
            } else {
                tunehigh += arpcalc;
            }
        }

        if (c.glideto != 0) {
            c.gototune = tunehigh;
            c.portspeed = c.glideto;
            c.glideto = c.finetune = 0;
            return;
        }

        setRegs((byte) (0x20 + regnum), (byte) sb.mod_misc);
        volcalc = sb.mod_vol;
        if (c.nextvol <= 0 || (sb.feedback & 1) <= 0) {
            c.volmod = volcalc;
        } else {
            c.volmod = (byte) ((volcalc & 0xc0) | ((((volcalc & 0x3f) * c.nextvol) >> 6)));
        }

        if ((sb.feedback & 1) == 1 && allvolume != 0) {
            setRegs((byte) (0x40 + regnum), (byte) (((c.volmod & 0xc0) | (((c.volmod & 0x3f) * allvolume) >> 8)) ^ 0x3f));
        } else {
            setRegs((byte) (0x40 + regnum), (byte) (c.volmod ^ 0x3f));
        }

        setRegs((byte) (0x60 + regnum), (byte) sb.mod_ad);
        setRegs((byte) (0x80 + regnum), (byte) sb.mod_sr);
        setRegs((byte) (0xe0 + regnum), (byte) sb.mod_wave);

        setRegs((byte) (0x23 + regnum), (byte) sb.car_misc);
        volcalc = sb.car_vol;
        if (c.nextvol <= 0) {
            c.volcar = volcalc;
        } else {
            c.volcar = (byte) ((volcalc & 0xc0) | ((((volcalc & 0x3f) * c.nextvol) >> 6)));
        }

        if (allvolume > 0) {
            setRegs((byte) (0x43 + regnum), (byte) (((c.volcar & 0xc0) | (((c.volcar & 0x3f) * allvolume) >> 8)) ^ 0x3f));
        } else {
            setRegs((byte) (0x43 + regnum), (byte) (c.volcar ^ 0x3f));
        }

        setRegs((byte) (0x63 + regnum), (byte) sb.car_ad);
        setRegs((byte) (0x83 + regnum), (byte) sb.car_sr);
        setRegs((byte) (0xe3 + regnum), (byte) sb.car_wave);
        setRegs((byte) (0xc0 + channel_number), (byte) sb.feedback);
        setRegsAdv((byte) (0xb0 + channel_number), (byte) 0xdf, (byte) 0);

        int freq = frequency[tunehigh % (12 * 16)];
        byte octave = (byte) (tunehigh / (12 * 16) - 1);
        if (sb.glide <= 0) {
            if (sb.portamento <= 0 || c.lasttune <= 0) {
                setRegs((byte) (0xa0 + channel_number), (byte) (freq & 0xff));
                setRegs((byte) (0xb0 + channel_number), (byte) ((octave << 2) + 0x20 + (freq >> 8)));
                c.lasttune = c.gototune = tunehigh;
            } else {
                c.gototune = tunehigh;
                c.portspeed = sb.portamento;
                setRegsAdv((byte) (0xb0 + channel_number), (byte) 0xdf, (byte) 0x20);
            }
        } else {
            setRegs((byte) (0xa0 + channel_number), (byte) (freq & 0xff));
            setRegs((byte) (0xb0 + channel_number), (byte) ((octave << 2) + 0x20 + (freq >> 8)));
            c.lasttune = tunehigh;
            c.gototune = tunehigh + ((sb.glide + 0x80) & 0xff) - 0x80;
            c.portspeed = sb.portamento;
        }

        if (sb.vibrato <= 0) {
            c.vibwait = c.vibspeed = c.vibrate = 0;
        } else {
            c.vibwait = sb.vibdelay;
            c.vibspeed = (sb.vibrato >> 4) + 2;
            c.vibrate = (sb.vibrato & 15) + 1;
        }

        if ((c.trmstay & 0xf0) <= 0) {
            c.trmwait = (sb.tremwait & 0xf0) >> 3;
            c.trmspeed = sb.mod_trem >> 4;
            c.trmrate = sb.mod_trem & 15;
            c.trmcount = 0;
        }

        if ((c.trmstay & 0x0f) <= 0) {
            c.trcwait = (sb.tremwait & 15) << 1;
            c.trcspeed = sb.car_trem >> 4;
            c.trcrate = sb.car_trem & 15;
            c.trccount = 0;
        }

        c.arp_size = sb.arpeggio & 15;
        c.arp_speed = sb.arpeggio >> 4;
        for (int j = 0; j < 12; j++) {
            c.arp_tab[j] = sb.arp_tab[j];
        }

        c.keycount = sb.keyoff;
        c.nextvol = c.glideto = c.finetune = c.vibcount = c.arp_pos = c.arp_count = 0;
    }

    private void setRegs(byte reg, byte val) {
        if (fmchip[reg & 0xff] == val) {
            return;
        }
        fmchip[reg & 0xff] = val;
        write(0, reg & 0xff, val & 0xff);
    }

    private void setRegsAdv(byte reg, byte mask, byte val) {
        setRegs(reg, (byte) ((fmchip[reg & 0xff] & mask) | val));
    }

    @Override
    public void rewind(int subsong) {
        tempo_now = 3;
        playing = true;
        songlooped = false;
        jumping = fadeonoff = allvolume = hardfade = pattplay = 0;
        posplay = jumppos = 0;
        mainvolume = 0;
        channel = new Channel[9];
        fmchip = new byte[255];

        write(0, 1, 0x20);
        write(0, 8, 0);
        write(0, 0xbd, regbd);

        for (int i = 0; i < 9; i++) {
            channel[i] = new Channel();
            write(0, 0x20 + op_table[i], 0);
            write(0, 0x23 + op_table[i], 0);
            write(0, 0x40 + op_table[i], 0x3f);
            write(0, 0x43 + op_table[i], 0x3f);
            write(0, 0x60 + op_table[i], 0xff);
            write(0, 0x63 + op_table[i], 0xff);
            write(0, 0x80 + op_table[i], 0xff);
            write(0, 0x83 + op_table[i], 0xff);
            write(0, 0xe0 + op_table[i], 0);
            write(0, 0xe3 + op_table[i], 0);
            write(0, 0xa0 + i, 0);
            write(0, 0xb0 + i, 0);
            write(0, 0xc0 + i, 0);
        }
    }

    @Override
    public float getRefresh() {
        return 1193182.0f / speed;
    }

    private static int u16(byte[] b, int off) {
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8);
    }
}
