/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2008 Simon Peter <dn.tlp@gmx.net>, et al.
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
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

/**
 * sopepos' Note Sequencer (SOP) Player.
 * Ported from adplug's sop.cpp / sop.h by Stas'M, with driver parts from
 * the free Ad262Sop Library (c) 1996-2005, Park Jeenhong.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class SopPlayer extends Opl3Player {

    // percussive voice numbers
    private static final int BD = 6;
    private static final int SD = 7;
    private static final int TOM = 8;
    private static final int CYMB = 9;
    private static final int HIHAT = 10;

    private static final int MAX_VOLUME = 0x7f;
    private static final int LOG2_VOLUME = 7;
    private static final int MID_C = 60;

    private static final int SOP_TOM_PITCH = 36;
    private static final int TOM_TO_SD = 7;
    private static final int SOP_SD_PITCH = SOP_TOM_PITCH + TOM_TO_SD;

    private static final int NB_NOTES = 96;
    private static final int NB_STEP_PITCH = 32;
    private static final int LOG_NB_STEP_PITCH = 5;

    private static final int maxVoices = 20;
    private static final int YMB_SIZE = 80;

    private static final int SOP_HEAD_SIZE = 76;
    private static final int SOP_DEF_TEMPO = 120;
    private static final int SOP_MAX_INST = 128;
    private static final int SOP_MAX_TRACK = 24;
    private static final int SOP_MAX_VOL = 127;

    private static final int SOP_CHAN_4OP = 1;

    private static final int SOP_INST_4OP = 0;
    private static final int SOP_INST_WAV = 11;
    private static final int SOP_INST_NONE = 12;

    private static final int SOP_EVNT_NOTE = 2;
    private static final int SOP_EVNT_TEMPO = 3;
    private static final int SOP_EVNT_VOL = 4;
    private static final int SOP_EVNT_PITCH = 5;
    private static final int SOP_EVNT_INST = 6;
    private static final int SOP_EVNT_PAN = 7;
    private static final int SOP_EVNT_MVOL = 8;

    /**
     * The Ad262Sop OPL3 driver.
     */
    private class Ad262Driver {

        private int percussion;
        private final int[] VolumeTable = new int[64 * 128];
        private final int[] voiceNote = new int[20];
        private final int[] voiceKeyOn = new int[20];
        private final int[] vPitchBend = new int[20];
        private final int[] Ksl = new int[20];
        private final int[] Ksl2 = new int[20];
        private final int[] Ksl2V = new int[20];
        private final int[] VoiceVolume = new int[20];
        private int OP_MASK;
        private final int[] ymbuf = new int[2 * YMB_SIZE];
        private final int[] OP4 = new int[20];
        private final int[] Stereo = new int[22];

        void SoundWarmInit() {
            for (int i = 0; i < 64; i++) {
                for (int j = 0; j < 128; j++) {
                    VolumeTable[i * 128 + j] = (i * j + (MAX_VOLUME + 1) / 2) >> LOG2_VOLUME;
                }
            }

            for (int i = 1; i <= 0xF5; i++) {
                SndOutput1(i, 0); // clear all registers
                SndOutput3(i, 0);
            }

            for (int i = 0; i < YMB_SIZE; i++) {
                ymbuf[i] = 0;
                ymbuf[i + YMB_SIZE] = 0;
            }

            for (int i = 0; i < 20; i++) {
                vPitchBend[i] = 100;
                voiceKeyOn[i] = 0;
                voiceNote[i] = MID_C;
                VoiceVolume[i] = 0;
                Ksl[i] = 0;
                Ksl2[i] = 0;
                Ksl2V[i] = 0;
                OP4[i] = 0;
                Stereo[i] = 0x30;
            }

            OP_MASK = 0;

            SndOutput1(4, 6); // mask T1 & T2

            SndOutput3(5, 1); // YMF-262M Mode
            SndOutput3(4, 0);

            SetMode_SOP(0); // melodic mode

            SndOutput1(0x08, 0);
            SndOutput1(1, 0x20);
        }

        void SetMode_SOP(int mode) {
            if (mode != 0) {
                // set the frequency for the last 4 percussion voices:
                voiceNote[TOM] = SOP_TOM_PITCH;
                vPitchBend[TOM] = 100;
                UpdateFNums(TOM);

                voiceNote[SD] = SOP_SD_PITCH;
                vPitchBend[SD] = 100;
                UpdateFNums(SD);
            }

            percussion = mode;

            SndOutput1(0xBD, percussion != 0 ? 0x20 : 0);
        }

        void UpdateFNums(int chan) {
            if (chan >= maxVoices) {
                return;
            }
            SetFreq_SOP(chan, voiceNote[chan], vPitchBend[chan], 0);
        }

        void SndOutput1(int addr, int value) {
            if (addr >= 0xB0) {
                ymbuf[addr - 0xB0] = value & 0xff;
            }
            write(0, addr & 0xff, value & 0xff);
        }

        void SndOutput3(int addr, int value) {
            if (addr >= 0xB0) {
                ymbuf[YMB_SIZE - 0xB0 + addr] = value & 0xff;
            }
            write(1, addr & 0xff, value & 0xff);
        }

        void SEND_INS(int baseAddr, int[] value, int off, int mode) {
            for (int i = 0; i < 4; i++) {
                write(mode, baseAddr & 0xff, value[off + i] & 0xff);
                baseAddr += 0x20;
            }

            baseAddr += 0x40;
            write(mode, baseAddr & 0xff, value[off + 4] & 0x07);
        }

        void SetYM_262_SOP(int vx) {
            SndOutput3(5, vx);
            SndOutput3(4, 0);
        }

        void SetStereoPan_SOP(int chan, int value) {
            int[] pan = { 0xa0, 0x30, 0x50 };
            int port, absPort;

            if (chan >= maxVoices || value < 0 || value >= 3) {
                return;
            }

            Stereo[chan] = value = pan[value];

            port = 0;

            if (chan < 9) {
                absPort = chan;
            } else {
                if (chan < 11) {
                    absPort = 17 - chan;
                } else {
                    absPort = chan - 11;
                    port = 1;
                }
            }

            value |= (chan >= 11) ? ymbuf[YMB_SIZE + 0x10 + absPort] & 0x0F : ymbuf[absPort + 0x10] & 0x0F;

            if (OP4[chan] != 0) {
                write(port, (absPort + 0xC3) & 0xff, ((value & 0xF0) | (((chan >= 11) ? ymbuf[YMB_SIZE + 0x13 + absPort] : ymbuf[absPort + 0x13]) & 0x0F)) & 0xff);
            }

            write(port, (absPort + 0xC0) & 0xff, value & 0xff);
        }

        void SetVoiceVolume_SOP(int chan, int vol) {
            int volume;
            int kslValue;

            if (chan >= maxVoices) {
                return;
            }

            if (chan > 2 && OP4[chan - 3] != 0) {
                return;
            }

            if (vol > MAX_VOLUME) {
                vol = MAX_VOLUME;
            }

            VoiceVolume[chan] = vol;

            if (Ksl2V[chan] != 0) {
                kslValue = Ksl2[chan];
                volume = 63 - VolumeTable[((63 - (kslValue & 0x3F)) << 7) + vol];

                if (chan >= 11) {
                    SndOutput3(VolReg[chan - 11] - 3, (kslValue & 0xC0) | volume);
                } else {
                    SndOutput1((percussion != 0 ? VolReg[chan + 11] : VolReg[chan]) - 3, (kslValue & 0xC0) | volume);
                }

                if (OP4[chan] != 0) {
                    chan += 3;
                    kslValue = Ksl[chan];
                    volume = 63 - VolumeTable[((63 - (kslValue & 0x3F)) << 7) + vol];

                    if (chan >= 11) {
                        SndOutput3(VolReg[chan - 11], (kslValue & 0xC0) | volume);
                    } else {
                        SndOutput1(VolReg[chan], (kslValue & 0xC0) | volume);
                    }

                    if (Ksl2V[chan] != 0) {
                        kslValue = Ksl2[chan];
                        volume = 63 - VolumeTable[((63 - (kslValue & 0x3F)) << 7) + vol];

                        if (chan >= 11) {
                            SndOutput3(VolReg[chan - 11] - 3, (kslValue & 0xC0) | volume);
                        } else {
                            SndOutput1(VolReg[chan] - 3, (kslValue & 0xC0) | volume);
                        }
                    }
                } else {
                    kslValue = Ksl[chan];
                    volume = 63 - VolumeTable[((63 - (kslValue & 0x3F)) << 7) + vol];

                    if (chan >= 11) {
                        SndOutput3(VolReg[chan - 11], (kslValue & 0xC0) | volume);
                    } else {
                        SndOutput1(percussion != 0 ? VolReg[chan + 11] : VolReg[chan], (kslValue & 0xC0) | volume);
                    }
                }
            } else {
                if (OP4[chan] != 0) {
                    kslValue = Ksl[chan + 3];
                    volume = 63 - VolumeTable[((63 - (kslValue & 0x3F)) << 7) + vol];

                    if (chan >= 11) {
                        SndOutput3(VolReg[chan + 3 - 11], (kslValue & 0xC0) | volume);
                    } else {
                        SndOutput1(VolReg[chan + 3], (kslValue & 0xC0) | volume);
                    }

                    if (Ksl2V[chan + 3] != 0) {
                        kslValue = Ksl[chan];
                        volume = 63 - VolumeTable[((63 - (kslValue & 0x3F)) << 7) + vol];

                        if (chan >= 11) {
                            SndOutput3(VolReg[chan - 11], (kslValue & 0xC0) | volume);
                        } else {
                            SndOutput1(VolReg[chan], (kslValue & 0xC0) | volume);
                        }
                    }
                } else {
                    kslValue = Ksl[chan];
                    volume = 63 - VolumeTable[((63 - (kslValue & 0x3F)) << 7) + vol];

                    if (chan >= 11) {
                        SndOutput3(VolReg[chan - 11], (kslValue & 0xC0) | volume);
                    } else {
                        SndOutput1(percussion != 0 ? VolReg[chan + 11] : VolReg[chan], (kslValue & 0xC0) | volume);
                    }
                }
            }
        }

        void SetVoiceTimbre_SOP(int chan, int[] array) {
            int i;
            int slotNumber, kslValue;

            if (chan >= maxVoices || (chan > 2 && OP4[chan - 3] != 0)) {
                return;
            }

            if (percussion == 0) {
                slotNumber = SlotX[chan];
            } else {
                slotNumber = SlotX[chan + 20];
            }

            kslValue = array[5] & 0x0F;
            Ksl2V[chan] = kslValue & 0x01;

            if (chan > 10) {
                i = chan + 0xC0 - 11;

                SndOutput3(i, 0);

                SEND_INS(0x20 + slotNumber, array, 0, 1);
                SEND_INS(0x23 + slotNumber, array, 6, 1);

                if (OP4[chan] != 0) {
                    SndOutput3(i + 3, 0);

                    SEND_INS(0x28 + slotNumber, array, 11, 1);
                    SEND_INS(0x2B + slotNumber, array, 17, 1);

                    Ksl[chan + 3] = array[18];
                    Ksl2[chan + 3] = array[12];
                    Ksl2V[chan + 3] = array[16] & 1;

                    SndOutput3(i + 3, (array[16] & 0x0F) | Stereo[chan]);
                }

                Ksl[chan] = array[7];
                Ksl2[chan] = array[1];
                Ksl2V[chan] = array[5] & 1;

                SetVoiceVolume_SOP(chan, VoiceVolume[chan]);
                SndOutput3(i, kslValue | Stereo[chan]);
            } else {
                if (chan > 8) {
                    i = 0xC0 + 17 - chan;
                } else {
                    i = chan + 0xC0;
                }

                SndOutput1(i, 0);

                SEND_INS(0x20 + slotNumber, array, 0, 0);

                if (percussion != 0 && chan > BD) {
                    Ksl[chan] = array[1];
                    Ksl2V[chan] = 0;
                } else {
                    SEND_INS(0x23 + slotNumber, array, 6, 0);

                    Ksl[chan] = array[7];
                    Ksl2[chan] = array[1];
                    Ksl2V[chan] = array[5] & 1;
                }

                if (OP4[chan] != 0) {
                    SndOutput1(i + 3, 0);

                    SEND_INS(0x28 + slotNumber, array, 11, 0);
                    SEND_INS(0x2B + slotNumber, array, 17, 0);

                    Ksl[chan + 3] = array[18];
                    Ksl2[chan + 3] = array[12];
                    Ksl2V[chan + 3] = array[16] & 1;

                    SndOutput1(i + 3, (array[16] & 0x0F) | Stereo[chan]);
                }

                SetVoiceVolume_SOP(chan, VoiceVolume[chan]);
                SndOutput1(i, kslValue | Stereo[chan]);
            }
        }

        void SetFreq_SOP(int voice, int note, int pitch, int keyOn) {
            int temp, fN, divFactor, fNIndex;

            temp = (int) ((pitch - 100) / 3.125) + ((note - 12) << LOG_NB_STEP_PITCH);

            if (temp < 0) {
                temp = 0;
            } else {
                if (temp >= ((NB_NOTES << LOG_NB_STEP_PITCH) - 1)) {
                    temp = (NB_NOTES << LOG_NB_STEP_PITCH) - 1;
                }
            }

            fNIndex = (MOD12[temp >> 5] << 5) + (temp & (NB_STEP_PITCH - 1));

            fN = fNumTbl[fNIndex];

            divFactor = DIV12[temp >> 5];

            if (voice <= 10) {
                SndOutput1(0xA0 + voice, fN & 0xFF);
            } else {
                SndOutput3(0xA0 + voice - 11, fN & 0xFF);
            }

            fN = (((fN >> 8) & 0x03) | (divFactor << 2) | keyOn) & 0xFF;

            if (voice <= 10) {
                SndOutput1(0xB0 + voice, fN);
            } else {
                SndOutput3(0xB0 + voice - 11, fN);
            }
        }

        void SetVoicePitch_SOP(int chan, int pitch) {
            if (chan >= maxVoices || pitch < 0 || pitch > 200) {
                return;
            }

            vPitchBend[chan] = pitch;

            if (percussion == 0) {
                SetFreq_SOP(chan, voiceNote[chan], pitch, voiceKeyOn[chan]);
            } else {
                if (chan <= BD || chan > HIHAT) {
                    SetFreq_SOP(chan, voiceNote[chan], pitch, voiceKeyOn[chan]);
                }
            }
        }

        void NoteOn_SOP(int chan, int pitch) {
            if (chan >= maxVoices) {
                return;
            }

            if (percussion != 0 && chan >= BD && chan <= HIHAT) {
                if (chan == BD) {
                    voiceNote[BD] = pitch;
                    SetFreq_SOP(BD, voiceNote[BD], vPitchBend[BD], 0);
                } else {
                    if (chan == TOM && voiceNote[TOM] != pitch) {
                        voiceNote[TOM] = pitch;
                        voiceNote[SD] = pitch + TOM_TO_SD;
                        SetFreq_SOP(TOM, voiceNote[TOM], 100, 0);
                        SetFreq_SOP(SD, voiceNote[SD], 100, 0);
                    }
                }
                SndOutput1(0xBD, ymbuf[0x0D] | (0x10 >> (chan - BD)));
            } else {
                voiceNote[chan] = pitch;
                voiceKeyOn[chan] = 0x20;

                SetFreq_SOP(chan, pitch, vPitchBend[chan], 0x20);
            }
        }

        void NoteOff_SOP(int chan) {
            if (chan >= maxVoices) {
                return;
            }

            voiceKeyOn[chan] = 0;

            if (percussion != 0 && chan >= BD && chan <= HIHAT) {
                SndOutput1(0xBD, ymbuf[0x0D] & (0xFF - (0x10 >> (chan - BD))));
            } else {
                if (chan < HIHAT) {
                    SndOutput1(0xB0 + chan, ymbuf[chan] & 0xDF);
                } else {
                    SndOutput3(0xB0 - 11 + chan, ymbuf[chan - 11 + YMB_SIZE] & 0xDF);
                }
            }
        }

        int Set_4OP_Mode(int chan, int value) {
            if (chan >= maxVoices) {
                return 1;
            }

            if (SlotX[chan + 20] <= 2) {
                OP4[chan] = value;

                if (value != 0) {
                    if (chan > 10) {
                        OP_MASK |= 0x01 << (chan - 11 + 3);
                    } else {
                        OP_MASK |= 0x01 << chan;
                    }
                } else {
                    if (chan > 10) {
                        OP_MASK &= 0xFF - (0x01 << (chan - 11 + 3));
                    } else {
                        OP_MASK &= 0xFF - (0x01 << chan);
                    }
                }

                SndOutput3(0x04, OP_MASK);

                return 1;
            }

            return 0;
        }
    }

    private static final int[] fNumTbl = {
        0x0159, 0x015A, 0x015A, 0x015B, 0x015C, 0x015C, 0x015D, 0x015D, 0x015E, 0x015F, 0x015F, 0x0160,
        0x0161, 0x0161, 0x0162, 0x0162, 0x0163, 0x0164, 0x0164, 0x0165, 0x0166, 0x0166, 0x0167, 0x0168,
        0x0168, 0x0169, 0x016A, 0x016A, 0x016B, 0x016C, 0x016C, 0x016D, 0x016E, 0x016E, 0x016F, 0x016F,
        0x0170, 0x0171, 0x0171, 0x0172, 0x0173, 0x0174, 0x0174, 0x0175, 0x0176, 0x0176, 0x0177, 0x0178,
        0x0178, 0x0179, 0x017A, 0x017A, 0x017B, 0x017C, 0x017C, 0x017D, 0x017E, 0x017E, 0x017F, 0x0180,
        0x0180, 0x0181, 0x0182, 0x0183, 0x0183, 0x0184, 0x0185, 0x0185, 0x0186, 0x0187, 0x0187, 0x0188,
        0x0189, 0x018A, 0x018A, 0x018B, 0x018C, 0x018C, 0x018D, 0x018E, 0x018F, 0x018F, 0x0190, 0x0191,
        0x0191, 0x0192, 0x0193, 0x0194, 0x0194, 0x0195, 0x0196, 0x0197, 0x0197, 0x0198, 0x0199, 0x019A,
        0x019A, 0x019B, 0x019C, 0x019D, 0x019D, 0x019E, 0x019F, 0x019F, 0x01A0, 0x01A1, 0x01A2, 0x01A3,
        0x01A3, 0x01A4, 0x01A5, 0x01A6, 0x01A6, 0x01A7, 0x01A8, 0x01A9, 0x01A9, 0x01AA, 0x01AB, 0x01AC,
        0x01AC, 0x01AD, 0x01AE, 0x01AF, 0x01B0, 0x01B0, 0x01B1, 0x01B2, 0x01B3, 0x01B3, 0x01B4, 0x01B5,
        0x01B6, 0x01B7, 0x01B7, 0x01B8, 0x01B9, 0x01BA, 0x01BB, 0x01BB, 0x01BC, 0x01BD, 0x01BE, 0x01BF,
        0x01BF, 0x01C0, 0x01C1, 0x01C2, 0x01C3, 0x01C3, 0x01C4, 0x01C5, 0x01C6, 0x01C7, 0x01C8, 0x01C8,
        0x01C9, 0x01CA, 0x01CB, 0x01CC, 0x01CD, 0x01CD, 0x01CE, 0x01CF, 0x01D0, 0x01D1, 0x01D2, 0x01D2,
        0x01D3, 0x01D4, 0x01D5, 0x01D6, 0x01D7, 0x01D7, 0x01D8, 0x01D9, 0x01DA, 0x01DB, 0x01DC, 0x01DD,
        0x01DD, 0x01DE, 0x01DF, 0x01E0, 0x01E1, 0x01E2, 0x01E3, 0x01E4, 0x01E4, 0x01E5, 0x01E6, 0x01E7,
        0x01E8, 0x01E9, 0x01EA, 0x01EB, 0x01EB, 0x01EC, 0x01ED, 0x01EE, 0x01EF, 0x01F0, 0x01F1, 0x01F2,
        0x01F3, 0x01F3, 0x01F4, 0x01F5, 0x01F6, 0x01F7, 0x01F8, 0x01F9, 0x01FA, 0x01FB, 0x01FC, 0x01FD,
        0x01FE, 0x01FE, 0x01FF, 0x0200, 0x0201, 0x0202, 0x0203, 0x0204, 0x0205, 0x0206, 0x0207, 0x0208,
        0x0209, 0x020A, 0x020B, 0x020B, 0x020C, 0x020D, 0x020E, 0x020F, 0x0210, 0x0211, 0x0212, 0x0213,
        0x0214, 0x0215, 0x0216, 0x0217, 0x0218, 0x0219, 0x021A, 0x021B, 0x021C, 0x021D, 0x021E, 0x021F,
        0x0220, 0x0221, 0x0222, 0x0223, 0x0224, 0x0225, 0x0226, 0x0227, 0x0228, 0x0229, 0x022A, 0x022B,
        0x022C, 0x022D, 0x022E, 0x022F, 0x0230, 0x0231, 0x0232, 0x0233, 0x0234, 0x0235, 0x0236, 0x0237,
        0x0238, 0x0239, 0x023A, 0x023B, 0x023C, 0x023D, 0x023E, 0x023F, 0x0240, 0x0241, 0x0242, 0x0243,
        0x0244, 0x0245, 0x0246, 0x0247, 0x0248, 0x0249, 0x024B, 0x024C, 0x024D, 0x024E, 0x024F, 0x0250,
        0x0251, 0x0252, 0x0253, 0x0254, 0x0255, 0x0256, 0x0257, 0x0258, 0x025A, 0x025B, 0x025C, 0x025D,
        0x025E, 0x025F, 0x0260, 0x0261, 0x0262, 0x0263, 0x0265, 0x0266, 0x0267, 0x0268, 0x0269, 0x026A,
        0x026B, 0x026C, 0x026D, 0x026F, 0x0270, 0x0271, 0x0272, 0x0273, 0x0274, 0x0275, 0x0276, 0x0278,
        0x0279, 0x027A, 0x027B, 0x027C, 0x027D, 0x027E, 0x0280, 0x0281, 0x0282, 0x0283, 0x0284, 0x0285,
        0x0287, 0x0288, 0x0289, 0x028A, 0x028B, 0x028C, 0x028E, 0x028F, 0x0290, 0x0291, 0x0292, 0x0294,
        0x0295, 0x0296, 0x0297, 0x0298, 0x029A, 0x029B, 0x029C, 0x029D, 0x029E, 0x02A0, 0x02A1, 0x02A2,
        0x02A3, 0x02A4, 0x02A6, 0x02A7, 0x02A8, 0x02A9, 0x02AB, 0x02AC, 0x02AD, 0x02AE, 0x02B0, 0x02B1
    };

    private static final int[] SlotX = {
        0, 1, 2, 8, 9, 10, 16, 17, 18, 0, 0, 0, 1, 2, 8, 9, 10, 16, 17, 18,
        0, 1, 2, 8, 9, 10, 16, 20, 18, 21, 17, 0, 1, 2, 8, 9, 10, 16, 17, 18
    };

    private static final int[] VolReg = {
        0x43, 0x44, 0x45, 0x4B, 0x4C, 0x4D, 0x53, 0x54, 0x55, 0, 0,
        0x43, 0x44, 0x45, 0x4B, 0x4C, 0x4D, 0x53, 0x54, 0x52, 0x55, 0x51
    };

    private static final int[] MOD12 = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
    };

    private static final int[] DIV12 = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
        4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
        5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7
    };

    private static class SopTrack {
        int nEvents;
        byte[] data;

        int pos;
        int counter;
        int ticks;
        int dur;
    }

    private Ad262Driver drv;

    private boolean songend;
    private float timer;
    private int version;
    private int curTempo;
    private final int[] volume = new int[SOP_MAX_TRACK];
    private final int[] lastvol = new int[SOP_MAX_TRACK];
    private int masterVol;

    private int percussive;
    private int tickBeat;
    private int basicTempo;
    private int nTracks;
    private int nInsts;

    private int[] chanMode;
    /** instrument data (SOP_INST4OP = 22 bytes each) */
    private int[][] instData;
    private int[] instType;
    private SopTrack[] track;

    @Override
    public Type getType() {
        return new Opl3FileFormatType("sopepos' Note Sequencer", "sop");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("SOP");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(10);
            byte[] hdr = new byte[10];
            if (bitStream.read(hdr) < 10) return false;
            String sign = new String(hdr, 0, 7, java.nio.charset.StandardCharsets.US_ASCII);
            if (!sign.equals("sopepos")) return false;
            int check = (hdr[7] & 0xff) | ((hdr[8] & 0xff) << 8) | ((hdr[9] & 0xff) << 16);
            return check == 0x100 || check == 0x200;
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
        byte[] buf = is.readAllBytes();
        if (buf.length < SOP_HEAD_SIZE) {
            throw new IllegalArgumentException("file too short");
        }

        // static header validation
        String sign = new String(buf, 0, 7, java.nio.charset.StandardCharsets.US_ASCII);
        if (!sign.equals("sopepos")) {
            throw new IllegalArgumentException("invalid signature");
        }
        int check = (buf[7] & 0xff) | ((buf[8] & 0xff) << 8) | ((buf[9] & 0xff) << 16);
        if (check != 0x100 && check != 0x200) {
            throw new IllegalArgumentException("unsupported version");
        }
        version = check;
        int offset = 10;
        offset += 13; // fname
        offset += 31; // title
        percussive = buf[offset++] & 0xff;
        check = buf[offset++] & 0xff;
        if (percussive > 1 || check != 0) {
            throw new IllegalArgumentException("invalid header");
        }
        tickBeat = buf[offset++] & 0xff;
        check = buf[offset++] & 0xff;
        if (tickBeat == 0 || check != 0) {
            throw new IllegalArgumentException("invalid header");
        }
        check = buf[offset++] & 0xff; // beatMeasure
        basicTempo = buf[offset++] & 0xff;
        if (basicTempo == 0) basicTempo = SOP_DEF_TEMPO;
        if (check == 0) {
            throw new IllegalArgumentException("invalid header");
        }
        offset += 13; // comment
        nTracks = buf[offset++] & 0xff;
        nInsts = buf[offset++] & 0xff;
        check = buf[offset++] & 0xff;
        if (nTracks == 0 || nInsts == 0 || nTracks > SOP_MAX_TRACK || nInsts > SOP_MAX_INST ||
                check != 0 || buf.length < SOP_HEAD_SIZE + nTracks) {
            throw new IllegalArgumentException("invalid header");
        }

        // dynamic data load

        // channel modes
        chanMode = new int[nTracks];
        for (int i = 0; i < nTracks; i++) {
            chanMode[i] = buf[offset++] & 0xff;
        }
        // instruments
        instData = new int[nInsts][22];
        instType = new int[nInsts];
        for (int i = 0; i < nInsts; i++) {
            instType[i] = buf[offset++] & 0xff;
            if (instType[i] > SOP_INST_NONE) {
                throw new IllegalArgumentException("invalid instrument type");
            }
            offset += 8;  // filename
            offset += 19; // longname
            if (instType[i] == SOP_INST_NONE) {
                continue;
            } else if (instType[i] == SOP_INST_WAV) {
                // sample record: val1(2) val2(2) length(2) val4(2) base_freq(2) val6(2) val7(2) val8(1) val9(2) val10(2)
                if (buf.length - offset < 19) {
                    throw new IllegalArgumentException("truncated sample");
                }
                int sampleLength = (buf[offset + 4] & 0xff) | ((buf[offset + 5] & 0xff) << 8);
                offset += 19;
                if (buf.length - offset < sampleLength) {
                    throw new IllegalArgumentException("truncated sample data");
                }
                offset += sampleLength;
                // inst data stays zero
            } else if (instType[i] == SOP_INST_4OP) {
                if (buf.length - offset < 22) {
                    throw new IllegalArgumentException("truncated instrument");
                }
                for (int j = 0; j < 22; j++) {
                    instData[i][j] = buf[offset++] & 0xff;
                }
            } else {
                if (buf.length - offset < 11) {
                    throw new IllegalArgumentException("truncated instrument");
                }
                for (int j = 0; j < 11; j++) {
                    instData[i][j] = buf[offset++] & 0xff;
                }
            }
        }
        // event tracks
        track = new SopTrack[nTracks + 1];
        for (int i = 0; i < nTracks + 1; i++) {
            track[i] = new SopTrack();
            track[i].nEvents = (buf[offset] & 0xff) | ((buf[offset + 1] & 0xff) << 8);
            offset += 2;
            long size = (buf[offset] & 0xffL) | ((buf[offset + 1] & 0xffL) << 8) |
                    ((buf[offset + 2] & 0xffL) << 16) | ((buf[offset + 3] & 0xffL) << 24);
            offset += 4;
            if (buf.length - offset < size) {
                throw new IllegalArgumentException("truncated track");
            }
            track[i].data = new byte[(int) size];
            System.arraycopy(buf, offset, track[i].data, 0, (int) size);
            offset += size;
        }

        drv = new Ad262Driver();
        rewind(0);
    }

    @Override
    public void rewind(int subSong) {
        // set default tempo
        setTempo(basicTempo);
        for (int i = 0; i < 256; i++) {
            write(0, i, 0);
            write(1, i, 0);
        }
        drv.SoundWarmInit();
        drv.SetYM_262_SOP(1);

        for (int i = 0; i < nTracks + 1; i++) {
            track[i].pos = 0;
            track[i].counter = 0;
            track[i].ticks = 0;
            track[i].dur = 0;
        }
        songend = false;

        for (int i = 0; i < SOP_MAX_TRACK; i++) {
            volume[i] = 0;
            lastvol[i] = 0;
        }
        masterVol = SOP_MAX_VOL;

        for (int i = 0; i < nTracks; i++) {
            if ((chanMode[i] & SOP_CHAN_4OP) != 0) {
                drv.Set_4OP_Mode(i, 1);
            }
        }
        drv.SetMode_SOP(percussive);
    }

    @Override
    public float getRefresh() {
        return timer;
    }

    /** sets tempo (BPM) */
    private void setTempo(int tempo) {
        if (tempo == 0) tempo = basicTempo;
        timer = tempo * tickBeat / 60.0f;
        curTempo = tempo;
    }

    /** executes SOP event (t - track index) */
    private void executeCommand(int t) {
        SopTrack trk = track[t];
        int event = trk.data[trk.pos++] & 0xff;
        int value;

        switch (event) {
        case SOP_EVNT_NOTE:
            if (trk.pos + 2 >= trk.data.length) break;
            value = trk.data[trk.pos++] & 0xff;
            trk.dur = trk.data[trk.pos++] & 0xff;
            trk.dur |= (trk.data[trk.pos++] & 0xff) << 8;
            // skip this event on control track, ignore notes with zero duration
            if (t == nTracks || trk.dur == 0) break;
            drv.NoteOn_SOP(t, value);
            break;
        case SOP_EVNT_TEMPO:
            if (trk.pos >= trk.data.length) break;
            value = trk.data[trk.pos++] & 0xff;
            // process this event only on control track
            if (t < nTracks) break;
            setTempo(value);
            break;
        case SOP_EVNT_VOL:
            if (trk.pos >= trk.data.length) break;
            value = trk.data[trk.pos++] & 0xff;
            // skip this event on control track
            if (t == nTracks) break;
            lastvol[t] = value;
            value = value * masterVol / SOP_MAX_VOL;
            if (value != volume[t]) {
                drv.SetVoiceVolume_SOP(t, value);
                volume[t] = value;
            }
            break;
        case SOP_EVNT_PITCH:
            if (trk.pos >= trk.data.length) break;
            value = trk.data[trk.pos++] & 0xff;
            // skip this event on control track
            if (t == nTracks) break;
            drv.SetVoicePitch_SOP(t, value);
            break;
        case SOP_EVNT_INST:
            if (trk.pos >= trk.data.length) break;
            value = trk.data[trk.pos++] & 0xff;
            // skip this event on control track, ignore values out of range
            if (t == nTracks || value >= nInsts) break;
            drv.SetVoiceTimbre_SOP(t, instData[value]);
            break;
        case SOP_EVNT_PAN:
            if (trk.pos >= trk.data.length) break;
            value = trk.data[trk.pos++] & 0xff;
            // skip this event on control track
            if (t == nTracks) break;
            if (version == 0x200) {
                switch (value) { // SOP v2 panning
                case 0x00: value = 2; break; // left
                case 0x40: value = 1; break; // middle
                case 0x80: value = 0; break; // right
                }
            }
            drv.SetStereoPan_SOP(t, value);
            break;
        case SOP_EVNT_MVOL:
            if (trk.pos >= trk.data.length) break;
            value = trk.data[trk.pos++] & 0xff;
            // process this event only on control track
            if (t < nTracks) break;
            masterVol = value;
            for (int i = 0; i < nTracks; i++) {
                value = lastvol[i] * masterVol / SOP_MAX_VOL;
                if (value != volume[i]) {
                    drv.SetVoiceVolume_SOP(i, value);
                    volume[i] = value;
                }
            }
            break;
        default:
            trk.pos++;
            break;
        }
    }

    @Override
    public boolean update() {
        songend = true;
        for (int i = 0; i < nTracks + 1; i++) {
            SopTrack trk = track[i];
            if (trk.dur != 0) {
                songend = false; // there are active notes
                if (--trk.dur == 0) {
                    drv.NoteOff_SOP(i);
                }
            }
            if (trk.pos >= trk.data.length) {
                continue;
            }
            songend = false; // track is not finished
            if (trk.counter == 0) {
                trk.ticks = trk.data[trk.pos++] & 0xff;
                trk.ticks |= (trk.data[trk.pos++] & 0xff) << 8;
                if (trk.pos == 2 && trk.ticks != 0) {
                    trk.ticks++; // workaround to synchronize tracks (there's always 1 excess tick at start)
                }
            }
            if (++trk.counter >= trk.ticks) {
                trk.counter = 0;
                while (trk.pos < trk.data.length) {
                    executeCommand(i);
                    if (trk.pos >= trk.data.length) {
                        break;
                    } else if (trk.pos + 1 < trk.data.length &&
                            trk.data[trk.pos] == 0 && trk.data[trk.pos + 1] == 0) { // if next delay is zero
                        trk.pos += 2;
                    } else {
                        break;
                    }
                }
            }
        }
        return !songend;
    }
}
