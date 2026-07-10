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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AdLib Visual Composer synth class.
 * Ported from adplug's composer.cpp / composer.h by OPLx.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public abstract class ComposerPlayer extends Opl3Player {

    protected static final int INS_MAX_NAME_SIZE = 9;
    protected static final int MAX_VOICES = 11;
    protected static final int ADLIB_OPER_LEN = 13;
    protected static final int ADLIB_INST_LEN = ADLIB_OPER_LEN * 2 + 2;

    protected static final int kNumMelodicVoices = 9;
    protected static final int kNumPercussiveVoices = 11;
    protected static final int kMidPitch = 0x2000;
    protected static final int kMaxVolume = 0x7F;

    private static final int skNrStepPitch = 25;
    private static final int skMaxNotes = 96;
    private static final int skCarrierOpOffset = 3;

    private static final int skOPL2_WaveCtrlBaseAddress = 0x01;
    private static final int skOPL2_AaMultiBaseAddress = 0x20;
    private static final int skOPL2_KSLTLBaseAddress = 0x40;
    private static final int skOPL2_ArDrBaseAddress = 0x60;
    private static final int skOPL2_SlrrBaseAddress = 0x80;
    private static final int skOPL2_FreqLoBaseAddress = 0xA0;
    private static final int skOPL2_KeyOnFreqHiBaseAddress = 0xB0;
    private static final int skOPL2_AmVibRhythmBaseAddress = 0xBD;
    private static final int skOPL2_FeedConBaseAddress = 0xC0;
    private static final int skOPL2_WaveformBaseAddress = 0xE0;

    private static final int skOPL2_EnableWaveformSelectMask = 0x20;
    private static final int skOPL2_KeyOnMask = 0x20;
    private static final int skOPL2_RhythmMask = 0x20;
    private static final int skOPL2_KSLMask = 0xC0;
    private static final int skOPL2_TLMask = 0x3F;
    private static final int skOPL2_TLMinLevel = 0x3F;
    private static final int skOPL2_FNumLSBMask = 0xFF;
    private static final int skOPL2_FNumMSBMask = 0x03;
    private static final int skOPL2_FNumMSBShift = 8;
    private static final int skOPL2_BlockNumberShift = 2;

    private static final int[] skNoteOctave = new int[skMaxNotes];
    private static final int[] skNoteIndex = new int[skMaxNotes];

    static {
        for (int i = 0; i < skMaxNotes; i++) {
            skNoteOctave[i] = i / 12;
            skNoteIndex[i] = i % 12;
        }
    }

    private static final int[][] skFNumNotes = {
        { 343, 364, 385, 408, 433, 459, 486, 515, 546, 579, 614, 650 },
        { 344, 365, 387, 410, 434, 460, 488, 517, 548, 581, 615, 652 },
        { 345, 365, 387, 410, 435, 461, 489, 518, 549, 582, 617, 653 },
        { 346, 366, 388, 411, 436, 462, 490, 519, 550, 583, 618, 655 },
        { 346, 367, 389, 412, 437, 463, 491, 520, 551, 584, 619, 657 },
        { 347, 368, 390, 413, 438, 464, 492, 522, 553, 586, 621, 658 },
        { 348, 369, 391, 415, 439, 466, 493, 523, 554, 587, 622, 660 },
        { 349, 370, 392, 415, 440, 467, 495, 524, 556, 589, 624, 661 },
        { 350, 371, 393, 416, 441, 468, 496, 525, 557, 590, 625, 663 },
        { 351, 372, 394, 417, 442, 469, 497, 527, 558, 592, 627, 665 },
        { 351, 372, 395, 418, 443, 470, 498, 528, 559, 593, 628, 666 },
        { 352, 373, 396, 419, 444, 471, 499, 529, 561, 594, 630, 668 },
        { 353, 374, 397, 420, 445, 472, 500, 530, 562, 596, 631, 669 },
        { 354, 375, 398, 421, 447, 473, 502, 532, 564, 597, 633, 671 },
        { 355, 376, 398, 422, 448, 474, 503, 533, 565, 599, 634, 672 },
        { 356, 377, 399, 423, 449, 475, 504, 534, 566, 600, 636, 674 },
        { 356, 378, 400, 424, 450, 477, 505, 535, 567, 601, 637, 675 },
        { 357, 379, 401, 425, 451, 478, 506, 537, 569, 603, 639, 677 },
        { 358, 379, 402, 426, 452, 479, 507, 538, 570, 604, 640, 679 },
        { 359, 380, 403, 427, 453, 480, 509, 539, 571, 606, 642, 680 },
        { 360, 381, 404, 428, 454, 481, 510, 540, 572, 607, 643, 682 },
        { 360, 382, 405, 429, 455, 482, 511, 541, 574, 608, 645, 683 },
        { 361, 383, 406, 430, 456, 483, 512, 543, 575, 610, 646, 685 },
        { 362, 384, 407, 431, 457, 484, 513, 544, 577, 611, 648, 687 },
        { 363, 385, 408, 432, 458, 485, 514, 545, 578, 612, 649, 688 }
    };

    private static final int[] op_table = {
        0x00, 0x01, 0x02, 0x08, 0x09, 0x0a, 0x10, 0x11, 0x12
    };

    private static final int[] drum_op_table = { 0x14, 0x12, 0x15, 0x11 };

    private static final int kSilenceNote = -12;
    private static final int kBassDrumChannel = 6;
    private static final int kSnareDrumChannel = 7;
    private static final int kTomtomChannel = 8;
    private static final int kTomTomNote = 24;
    private static final int kTomTomToSnare = 7;
    private static final int kSnareNote = kTomTomNote + kTomTomToSnare;

    protected static class SOPL2Op {
        int ammulti;
        int ksltl;
        int ardr;
        int slrr;
        int fbc;
        int waveform;
    }

    protected static class SInstrumentData {
        int mode;
        int voice_number;
        final SOPL2Op modulator = new SOPL2Op();
        final SOPL2Op carrier = new SOPL2Op();
    }

    protected static class SInstrument {
        String name = "";
        final SInstrumentData instrument = new SInstrumentData();
    }

    protected final List<SInstrument> mInstrumentList = new ArrayList<>();
    private final int[] fnumFreqTableIndex = new int[kNumPercussiveVoices];
    protected final int[] mHalfToneOffset = new int[kNumPercussiveVoices];
    protected final int[] mVolumeCache = new int[kNumPercussiveVoices];
    private final int[] mKSLTLCache = new int[kNumPercussiveVoices];
    private final int[] mNoteCache = new int[kNumPercussiveVoices];
    private final int[] mKOnOctFNumCache = new int[kNumMelodicVoices];
    protected final boolean[] mKeyOnCache = new boolean[kNumPercussiveVoices];
    protected int mRhythmMode;
    private int mPitchRangeStep = skNrStepPitch;
    private int mAMVibRhythmCache;

    public ComposerPlayer() {
        super();
    }

    @Override
    public void rewind(int subsong) throws IOException {
        Arrays.fill(mHalfToneOffset, 0);
        Arrays.fill(mVolumeCache, kMaxVolume);
        Arrays.fill(mKSLTLCache, 0);
        Arrays.fill(mNoteCache, 0);
        Arrays.fill(mKOnOctFNumCache, 0);
        Arrays.fill(mKeyOnCache, false);
        Arrays.fill(fnumFreqTableIndex, 0);
        mAMVibRhythmCache = 0;

        write(0, skOPL2_WaveCtrlBaseAddress, skOPL2_EnableWaveformSelectMask);

        frontend_rewind(subsong);
    }

    protected abstract void frontend_rewind(int subsong);

    protected void SetRhythmMode(int mode) {
        if (mode != 0) {
            mAMVibRhythmCache |= skOPL2_RhythmMask;
            write(0, skOPL2_AmVibRhythmBaseAddress, mAMVibRhythmCache);

            SetFreq(kTomtomChannel, kTomTomNote, false);
            SetFreq(kSnareDrumChannel, kSnareNote, false);
        } else {
            mAMVibRhythmCache &= ~skOPL2_RhythmMask;
            write(0, skOPL2_AmVibRhythmBaseAddress, mAMVibRhythmCache);
        }

        mRhythmMode = mode;
    }

    protected void SetNote(int voice, int note) {
        if (voice < kBassDrumChannel || mRhythmMode == 0) {
            SetNoteMelodic(voice, note);
        } else {
            SetNotePercussive(voice, note);
        }
    }

    protected void NoteOn(int voice, int note) {
        SetNote(voice, note + kSilenceNote);
    }

    protected void NoteOff(int voice) {
        SetNote(voice, kSilenceNote);
    }

    private void SetNotePercussive(int voice, int note) {
        int channel_bit_mask = 1 << (4 - voice + kBassDrumChannel);

        mAMVibRhythmCache &= ~channel_bit_mask;
        write(0, skOPL2_AmVibRhythmBaseAddress, mAMVibRhythmCache);
        mKeyOnCache[voice] = false;

        if (note != kSilenceNote) {
            switch (voice) {
                case kTomtomChannel -> {
                    SetFreq(kTomtomChannel, note, false);
                    SetFreq(kSnareDrumChannel, note + kTomTomToSnare, false);
                }
                case kBassDrumChannel -> SetFreq(voice, note, false);
            }

            mKeyOnCache[voice] = true;
            mAMVibRhythmCache |= channel_bit_mask;
            write(0, skOPL2_AmVibRhythmBaseAddress, mAMVibRhythmCache);
        }
    }

    private void SetNoteMelodic(int voice, int note) {
        if (voice >= kNumMelodicVoices) {
            return;
        }
        write(0, skOPL2_KeyOnFreqHiBaseAddress + voice, mKOnOctFNumCache[voice] & ~skOPL2_KeyOnMask);
        mKeyOnCache[voice] = false;

        if (note != kSilenceNote) {
            SetFreq(voice, note, true);
        }
    }

    protected void SetPitchRange(int pitchRange) {
        if (pitchRange > 12) pitchRange = 12;
        if (pitchRange < 1) pitchRange = 1;
        mPitchRangeStep = pitchRange * skNrStepPitch;
    }

    protected void ChangePitch(int voice, int pitchBend) {
        int pitchBendLength = (pitchBend - kMidPitch) * mPitchRangeStep;

        if (voice >= kBassDrumChannel && mRhythmMode != 0) {
            return;
        }

        // kMidPitch is uint32_t in adplug, so this division is unsigned there;
        // combined with the int16_t cast that amounts to floor division
        int pitchStepDir = Math.floorDiv(pitchBendLength, kMidPitch);
        int delta;
        if (pitchStepDir < 0) {
            int pitchStepDown = skNrStepPitch - 1 - pitchStepDir;
            mHalfToneOffset[voice] = -(pitchStepDown / skNrStepPitch);
            delta = (pitchStepDown - skNrStepPitch + 1) % skNrStepPitch;
            if (delta != 0) {
                delta = skNrStepPitch - delta;
            }
        } else {
            mHalfToneOffset[voice] = pitchStepDir / skNrStepPitch;
            delta = pitchStepDir % skNrStepPitch;
        }

        fnumFreqTableIndex[voice] = delta;

        SetFreq(voice, mNoteCache[voice], mKeyOnCache[voice]);
    }

    private void SetFreq(int voice, int note, boolean keyOn) {
        int biased_note = Math.max(0, Math.min(skMaxNotes - 1, note + mHalfToneOffset[voice]));
        int frequency = skFNumNotes[fnumFreqTableIndex[voice]][skNoteIndex[biased_note]];

        mNoteCache[voice] = note;
        mKeyOnCache[voice] = keyOn;

        mKOnOctFNumCache[voice] = (skNoteOctave[biased_note] << skOPL2_BlockNumberShift) | ((frequency >> skOPL2_FNumMSBShift) & skOPL2_FNumMSBMask);

        write(0, skOPL2_FreqLoBaseAddress + voice, frequency & skOPL2_FNumLSBMask);
        write(0, skOPL2_KeyOnFreqHiBaseAddress + voice, mKOnOctFNumCache[voice] | (keyOn ? skOPL2_KeyOnMask : 0x0));
    }

    private int GetKSLTL(int voice) {
        int kslTL = skOPL2_TLMinLevel - (mKSLTLCache[voice] & skOPL2_TLMask);
        kslTL = mVolumeCache[voice] * kslTL;
        kslTL += kslTL + kMaxVolume;
        kslTL = skOPL2_TLMinLevel - (kslTL / (2 * kMaxVolume));
        kslTL |= mKSLTLCache[voice] & skOPL2_KSLMask;
        return kslTL & 0xff;
    }

    protected void SetVolume(int voice, int volume) {
        if (voice >= kNumMelodicVoices && mRhythmMode == 0) {
            return;
        }
        int op_offset = (voice < kSnareDrumChannel || mRhythmMode == 0) ? op_table[voice] + skCarrierOpOffset : drum_op_table[voice - kSnareDrumChannel];

        mVolumeCache[voice] = volume;

        write(0, skOPL2_KSLTLBaseAddress + op_offset, GetKSLTL(voice));
    }

    protected void SetInstrument(int voice, int ins_index) {
        if (voice >= kNumMelodicVoices && mRhythmMode == 0) {
            return;
        }
        SInstrumentData instrument = mInstrumentList.get(ins_index).instrument;
        send_operator(voice, instrument.modulator, instrument.carrier);
    }

    protected void SetDefaultInstrument(int voice) {
        int[] pianoParamsOp0 = { 1, 1, 3, 15, 5, 0, 1, 3, 15, 0, 0, 0, 1, 0 };
        int[] pianoParamsOp1 = { 0, 1, 1, 15, 7, 0, 2, 4, 0, 0, 0, 1, 0, 0 };

        int[] bdOpr0 = { 0, 0, 0, 10, 4, 0, 8, 12, 11, 0, 0, 0, 1, 0 };
        int[] bdOpr1 = { 0, 0, 0, 13, 4, 0, 6, 15, 0, 0, 0, 0, 1, 0 };
        int[] sdOpr = { 0, 12, 0, 15, 11, 0, 8, 5, 0, 0, 0, 0, 0, 0 };
        int[] tomOpr = { 0, 4, 0, 15, 11, 0, 7, 5, 0, 0, 0, 0, 0, 0 };
        int[] cymbOpr = { 0, 1, 0, 15, 11, 0, 5, 5, 0, 0, 0, 0, 0, 0 };
        int[] hhOpr = { 0, 1, 0, 15, 11, 0, 7, 5, 0, 0, 0, 0, 0, 0 };

        byte[] data = new byte[ADLIB_INST_LEN];

        for (int i = 0; i < pianoParamsOp0.length - 1; i++) {
            if (voice < kBassDrumChannel || mRhythmMode == 0) {
                data[i] = (byte) pianoParamsOp0[i];
                data[ADLIB_OPER_LEN + i] = (byte) pianoParamsOp1[i];
            } else if (voice == kBassDrumChannel) {
                data[i] = (byte) bdOpr0[i];
                data[ADLIB_OPER_LEN + i] = (byte) bdOpr1[i];
            } else if (voice == kSnareDrumChannel) {
                data[i] = (byte) sdOpr[i];
            } else if (voice == kTomtomChannel) {
                data[i] = (byte) tomOpr[i];
            } else if (voice == kTomtomChannel + 1) {
                data[i] = (byte) cymbOpr[i];
            } else if (voice == kTomtomChannel + 2) {
                data[i] = (byte) hhOpr[i];
            }
        }

        int index = load_instrument_data(data, data.length);
        SetInstrument(voice, index);
    }

    private void send_operator(int voice, SOPL2Op modulator, SOPL2Op carrier) {
        if (voice < kSnareDrumChannel || mRhythmMode == 0) {
            if (voice >= kNumMelodicVoices) {
                return;
            }
            int op_offset = op_table[voice];

            write(0, skOPL2_AaMultiBaseAddress + op_offset, modulator.ammulti);
            write(0, skOPL2_KSLTLBaseAddress + op_offset, modulator.ksltl);
            write(0, skOPL2_ArDrBaseAddress + op_offset, modulator.ardr);
            write(0, skOPL2_SlrrBaseAddress + op_offset, modulator.slrr);
            write(0, skOPL2_FeedConBaseAddress + voice, modulator.fbc);
            write(0, skOPL2_WaveformBaseAddress + op_offset, modulator.waveform);

            mKSLTLCache[voice] = carrier.ksltl;

            write(0, skOPL2_AaMultiBaseAddress + op_offset + skCarrierOpOffset, carrier.ammulti);
            write(0, skOPL2_KSLTLBaseAddress + op_offset + skCarrierOpOffset, GetKSLTL(voice));
            write(0, skOPL2_ArDrBaseAddress + op_offset + skCarrierOpOffset, carrier.ardr);
            write(0, skOPL2_SlrrBaseAddress + op_offset + skCarrierOpOffset, carrier.slrr);
            write(0, skOPL2_WaveformBaseAddress + op_offset + skCarrierOpOffset, carrier.waveform);
        } else {
            int op_offset = drum_op_table[voice - kSnareDrumChannel];

            mKSLTLCache[voice] = modulator.ksltl;

            write(0, skOPL2_AaMultiBaseAddress + op_offset, modulator.ammulti);
            write(0, skOPL2_KSLTLBaseAddress + op_offset, GetKSLTL(voice));
            write(0, skOPL2_ArDrBaseAddress + op_offset, modulator.ardr);
            write(0, skOPL2_SlrrBaseAddress + op_offset, modulator.slrr);
            write(0, skOPL2_WaveformBaseAddress + op_offset, modulator.waveform);
        }
    }

    protected int load_instrument_data(byte[] data, int size) {
        if (size > ADLIB_INST_LEN) {
            size = ADLIB_INST_LEN;
        }

        SInstrument i = new SInstrument();
        read_bnk_instrument(data, i.instrument, true);

        for (int index = 0; index < mInstrumentList.size(); ++index) {
            if (instrumentEquals(mInstrumentList.get(index).instrument, i.instrument)) {
                return index;
            }
        }

        mInstrumentList.add(i);
        return mInstrumentList.size() - 1;
    }

    private boolean instrumentEquals(SInstrumentData a, SInstrumentData b) {
        return a.mode == b.mode &&
               a.voice_number == b.voice_number &&
               opEquals(a.modulator, b.modulator) &&
               opEquals(a.carrier, b.carrier);
    }

    private boolean opEquals(SOPL2Op a, SOPL2Op b) {
        return a.ammulti == b.ammulti &&
               a.ksltl == b.ksltl &&
               a.ardr == b.ardr &&
               a.slrr == b.slrr &&
               a.fbc == b.fbc &&
               a.waveform == b.waveform;
    }

    private void read_bnk_instrument(byte[] data, SInstrumentData instrument, boolean raw) {
        int off = 0;
        instrument.mode = raw ? 0 : data[off++] & 0xff;
        instrument.voice_number = raw ? 0 : data[off++] & 0xff;

        read_fm_operator(data, off, instrument.modulator);
        off += 13;
        read_fm_operator(data, off, instrument.carrier);
        off += 13;

        instrument.modulator.waveform = data[off++] & 0xff;
        instrument.carrier.waveform = data[off++] & 0xff;
    }

    private void read_fm_operator(byte[] data, int off, SOPL2Op opl2_op) {
        int key_scale_level = data[off] & 0xff;
        int freq_multiplier = data[off + 1] & 0xff;
        int feed_back = data[off + 2] & 0xff;
        int attack_rate = data[off + 3] & 0xff;
        int sustain_level = data[off + 4] & 0xff;
        int sustaining_sound = data[off + 5] & 0xff;
        int decay_rate = data[off + 6] & 0xff;
        int release_rate = data[off + 7] & 0xff;
        int output_level = data[off + 8] & 0xff;
        int amplitude_vibrato = data[off + 9] & 0xff;
        int frequency_vibrato = data[off + 10] & 0xff;
        int envelope_scaling = data[off + 11] & 0xff;
        int fm_type = data[off + 12] & 0xff;

        opl2_op.ammulti = (amplitude_vibrato << 7) | (frequency_vibrato << 6) | (sustaining_sound << 5) | (envelope_scaling << 4) | freq_multiplier;
        opl2_op.ksltl = (key_scale_level << 6) | output_level;
        opl2_op.ardr = (attack_rate << 4) | decay_rate;
        opl2_op.slrr = (sustain_level << 4) | release_rate;
        opl2_op.fbc = (feed_back << 1) | (fm_type ^ 1);
    }
}
