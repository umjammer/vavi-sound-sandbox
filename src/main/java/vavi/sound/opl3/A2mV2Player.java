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
import java.net.URI;
import java.util.Arrays;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;
import vavi.sound.SoundUtil;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

/**
 * Adlib Tracker II Player (module versions 1 - 14).
 * Ported from adplug's a2m-v2.cpp / a2m-v2.h by Dmitry Smagin, originally
 * by Stanislav Baranec, adapted from the FreePascal sources of AT2.
 *
 * In adplug the old loader ({@link A2mPlayer}) is registered first and
 * takes A2M versions 1-8; this player takes A2M v9-14 and all A2T files.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class A2mV2Player extends Opl3Player {

    private static final int BYTE_NULL = 0xff;
    private static final int MIN_IRQ_FREQ = 50;
    private static final int MAX_IRQ_FREQ = 1000;

    private static final int keyoff_flag = 0x80;
    private static final int fixed_note_flag = 0x90;
    private static final int pattern_loop_flag = 0xe0;
    private static final int pattern_break_flag = 0xf0;

    private static final int IDLE = 0xfff;
    private static final int FINISHED = 0xffff;

    // effect numbers
    private static final int ef_Arpeggio = 0;
    private static final int ef_FSlideUp = 1;
    private static final int ef_FSlideDown = 2;
    private static final int ef_TonePortamento = 3;
    private static final int ef_Vibrato = 4;
    private static final int ef_TPortamVolSlide = 5;
    private static final int ef_VibratoVolSlide = 6;
    private static final int ef_FSlideUpFine = 7;
    private static final int ef_FSlideDownFine = 8;
    private static final int ef_SetModulatorVol = 9;
    private static final int ef_VolSlide = 10;
    private static final int ef_PositionJump = 11;
    private static final int ef_SetInsVolume = 12;
    private static final int ef_PatternBreak = 13;
    private static final int ef_SetTempo = 14;
    private static final int ef_SetSpeed = 15;
    private static final int ef_TPortamVSlideFine = 16;
    private static final int ef_VibratoVSlideFine = 17;
    private static final int ef_SetCarrierVol = 18;
    private static final int ef_SetWaveform = 19;
    private static final int ef_VolSlideFine = 20;
    private static final int ef_RetrigNote = 21;
    private static final int ef_Tremolo = 22;
    private static final int ef_Tremor = 23;
    private static final int ef_ArpggVSlide = 24;
    private static final int ef_ArpggVSlideFine = 25;
    private static final int ef_MultiRetrigNote = 26;
    private static final int ef_FSlideUpVSlide = 27;
    private static final int ef_FSlideDownVSlide = 28;
    private static final int ef_FSlUpFineVSlide = 29;
    private static final int ef_FSlDownFineVSlide = 30;
    private static final int ef_FSlUpVSlF = 31;
    private static final int ef_FSlDownVSlF = 32;
    private static final int ef_FSlUpFineVSlF = 33;
    private static final int ef_FSlDownFineVSlF = 34;
    private static final int ef_Extended = 35;
    private static final int ef_Extended2 = 36;
    private static final int ef_SetGlobalVolume = 37;
    private static final int ef_SwapArpeggio = 38;
    private static final int ef_SwapVibrato = 39;
    private static final int ef_ForceInsVolume = 40;
    private static final int ef_Extended3 = 41;
    private static final int ef_ExtraFineArpeggio = 42;
    private static final int ef_ExtraFineVibrato = 43;
    private static final int ef_ExtraFineTremolo = 44;
    private static final int ef_SetCustomSpeedTab = 45;
    private static final int ef_GlobalFSlideUp = 46;
    private static final int ef_GlobalFSlideDown = 47;
    private static final int ef_GlobalFreqSlideUpXF = 48;
    private static final int ef_GlobalFreqSlideDnXF = 49;

    private static final int ef_ex_SetTremDepth = 0;
    private static final int ef_ex_SetVibDepth = 1;
    private static final int ef_ex_SetAttckRateM = 2;
    private static final int ef_ex_SetDecayRateM = 3;
    private static final int ef_ex_SetSustnLevelM = 4;
    private static final int ef_ex_SetRelRateM = 5;
    private static final int ef_ex_SetAttckRateC = 6;
    private static final int ef_ex_SetDecayRateC = 7;
    private static final int ef_ex_SetSustnLevelC = 8;
    private static final int ef_ex_SetRelRateC = 9;
    private static final int ef_ex_SetFeedback = 10;
    private static final int ef_ex_SetPanningPos = 11;
    private static final int ef_ex_PatternLoop = 12;
    private static final int ef_ex_PatternLoopRec = 13;
    private static final int ef_ex_ExtendedCmd = 14;
    private static final int ef_ex_cmd_MKOffLoopDi = 0;
    private static final int ef_ex_cmd_MKOffLoopEn = 1;
    private static final int ef_ex_cmd_TPortaFKdis = 2;
    private static final int ef_ex_cmd_TPortaFKenb = 3;
    private static final int ef_ex_cmd_RestartEnv = 4;
    private static final int ef_ex_cmd_4opVlockOff = 5;
    private static final int ef_ex_cmd_4opVlockOn = 6;
    private static final int ef_ex_cmd_ForceBpmSld = 7;
    private static final int ef_ex_ExtendedCmd2 = 15;
    private static final int ef_ex_cmd2_RSS = 0;
    private static final int ef_ex_cmd2_ResetVol = 1;
    private static final int ef_ex_cmd2_LockVol = 2;
    private static final int ef_ex_cmd2_UnlockVol = 3;
    private static final int ef_ex_cmd2_LockVP = 4;
    private static final int ef_ex_cmd2_UnlockVP = 5;
    private static final int ef_ex_cmd2_VSlide_mod = 6;
    private static final int ef_ex_cmd2_VSlide_car = 7;
    private static final int ef_ex_cmd2_VSlide_def = 8;
    private static final int ef_ex_cmd2_LockPan = 9;
    private static final int ef_ex_cmd2_UnlockPan = 10;
    private static final int ef_ex_cmd2_VibrOff = 11;
    private static final int ef_ex_cmd2_TremOff = 12;
    private static final int ef_ex_cmd2_FVib_FGFS = 13;
    private static final int ef_ex_cmd2_FTrm_XFGFS = 14;
    private static final int ef_ex_cmd2_NoRestart = 15;
    private static final int ef_ex2_PatDelayFrame = 0;
    private static final int ef_ex2_PatDelayRow = 1;
    private static final int ef_ex2_NoteDelay = 2;
    private static final int ef_ex2_NoteCut = 3;
    private static final int ef_ex2_FineTuneUp = 4;
    private static final int ef_ex2_FineTuneDown = 5;
    private static final int ef_ex2_GlVolSlideUp = 6;
    private static final int ef_ex2_GlVolSlideDn = 7;
    private static final int ef_ex2_GlVolSlideUpF = 8;
    private static final int ef_ex2_GlVolSlideDnF = 9;
    private static final int ef_ex2_GlVolSldUpXF = 10;
    private static final int ef_ex2_GlVolSldDnXF = 11;
    private static final int ef_ex2_VolSlideUpXF = 12;
    private static final int ef_ex2_VolSlideDnXF = 13;
    private static final int ef_ex2_FreqSlideUpXF = 14;
    private static final int ef_ex2_FreqSlideDnXF = 15;
    private static final int ef_ex3_SetConnection = 0;
    private static final int ef_ex3_SetMultipM = 1;
    private static final int ef_ex3_SetKslM = 2;
    private static final int ef_ex3_SetTremoloM = 3;
    private static final int ef_ex3_SetVibratoM = 4;
    private static final int ef_ex3_SetKsrM = 5;
    private static final int ef_ex3_SetSustainM = 6;
    private static final int ef_ex3_SetMultipC = 7;
    private static final int ef_ex3_SetKslC = 8;
    private static final int ef_ex3_SetTremoloC = 9;
    private static final int ef_ex3_SetVibratoC = 10;
    private static final int ef_ex3_SetKsrC = 11;
    private static final int ef_ex3_SetSustainC = 12;

    private static final int EFGR_ARPVOLSLIDE = 1;
    private static final int EFGR_FSLIDEVOLSLIDE = 2;
    private static final int EFGR_TONEPORTAMENTO = 3;
    private static final int EFGR_VIBRATO = 4;
    private static final int EFGR_TREMOLO = 5;
    private static final int EFGR_VIBRATOVOLSLIDE = 6;
    private static final int EFGR_PORTAVOLSLIDE = 7;
    private static final int EFGR_RETRIGNOTE = 8;

    // old v1234 effects
    private static final int fx_Arpeggio = 0x00;
    private static final int fx_FSlideUp = 0x01;
    private static final int fx_FSlideDown = 0x02;
    private static final int fx_FSlideUpFine = 0x03;
    private static final int fx_FSlideDownFine = 0x04;
    private static final int fx_TonePortamento = 0x05;
    private static final int fx_TPortamVolSlide = 0x06;
    private static final int fx_Vibrato = 0x07;
    private static final int fx_VibratoVolSlide = 0x08;
    private static final int fx_SetOpIntensity = 0x09;
    private static final int fx_SetInsVolume = 0x0a;
    private static final int fx_PatternBreak = 0x0b;
    private static final int fx_PatternJump = 0x0c;
    private static final int fx_SetTempo = 0x0d;
    private static final int fx_SetTimer = 0x0e;
    private static final int fx_Extended = 0x0f;
    private static final int fx_ex_DefAMdepth = 0x00;
    private static final int fx_ex_DefVibDepth = 0x01;
    private static final int fx_ex_DefWaveform = 0x02;
    private static final int fx_ex_ManSlideUp = 0x03;
    private static final int fx_ex_ManSlideDown = 0x04;
    private static final int fx_ex_VSlideUp = 0x05;
    private static final int fx_ex_VSlideDown = 0x06;
    private static final int fx_ex_VSlideUpFine = 0x07;
    private static final int fx_ex_VSlideDownFine = 0x08;
    private static final int fx_ex_RetrigNote = 0x09;
    private static final int fx_ex_SetAttckRate = 0x0a;
    private static final int fx_ex_SetDecayRate = 0x0b;
    private static final int fx_ex_SetSustnLevel = 0x0c;
    private static final int fx_ex_SetReleaseRate = 0x0d;
    private static final int fx_ex_SetFeedback = 0x0e;
    private static final int fx_ex_ExtendedCmd = 0x0f;

    private static final int FreqStart = 0x156;
    private static final int FreqEnd = 0x2ae;
    private static final int FreqRange = FreqEnd - FreqStart;

    private static final int[] _panning = { 0x30, 0x10, 0x20 };

    private static final int[] def_vibtrem_table = new int[256];
    static {
        int[] pattern = {
            0, 24, 49, 74, 97, 120, 141, 161, 180, 197, 212, 224, 235, 244, 250, 253, 255,
            253, 250, 244, 235, 224, 212, 197, 180, 161, 141, 120, 97, 74, 49, 24
        };
        for (int i = 0; i < 8; i++) {
            System.arraycopy(pattern, 0, def_vibtrem_table, i * 32, 32);
        }
    }

    // ---- data structures

    /**
     * tFM_INST_DATA - 11 bytes of raw FM data with bitfield accessors
     * (little-endian LSB-first bitfields as in FreePascal/gcc).
     */
    private static class FmInstData {
        final int[] data = new int[11];

        void copyFrom(FmInstData o) {
            System.arraycopy(o.data, 0, data, 0, 11);
        }

        int multipM() { return data[0] & 0x0f; }
        int ksrM()    { return (data[0] >> 4) & 1; }
        int sustM()   { return (data[0] >> 5) & 1; }
        int vibrM()   { return (data[0] >> 6) & 1; }
        int tremM()   { return (data[0] >> 7) & 1; }
        int multipC() { return data[1] & 0x0f; }
        int ksrC()    { return (data[1] >> 4) & 1; }
        int sustC()   { return (data[1] >> 5) & 1; }
        int vibrC()   { return (data[1] >> 6) & 1; }
        int tremC()   { return (data[1] >> 7) & 1; }
        int volM()    { return data[2] & 0x3f; }
        int kslM()    { return (data[2] >> 6) & 3; }
        int volC()    { return data[3] & 0x3f; }
        int kslC()    { return (data[3] >> 6) & 3; }
        int decM()    { return data[4] & 0x0f; }
        int attckM()  { return (data[4] >> 4) & 0x0f; }
        int decC()    { return data[5] & 0x0f; }
        int attckC()  { return (data[5] >> 4) & 0x0f; }
        int relM()    { return data[6] & 0x0f; }
        int sustnM()  { return (data[6] >> 4) & 0x0f; }
        int relC()    { return data[7] & 0x0f; }
        int sustnC()  { return (data[7] >> 4) & 0x0f; }
        int wformM()  { return data[8] & 0x07; }
        int wformC()  { return data[9] & 0x07; }
        int connect() { return data[10] & 1; }
        int feedb()   { return (data[10] >> 1) & 0x07; }

        void multipM(int v) { data[0] = (data[0] & ~0x0f) | (v & 0x0f); }
        void ksrM(int v)    { data[0] = (data[0] & ~0x10) | ((v & 1) << 4); }
        void sustM(int v)   { data[0] = (data[0] & ~0x20) | ((v & 1) << 5); }
        void vibrM(int v)   { data[0] = (data[0] & ~0x40) | ((v & 1) << 6); }
        void tremM(int v)   { data[0] = (data[0] & ~0x80) | ((v & 1) << 7); }
        void multipC(int v) { data[1] = (data[1] & ~0x0f) | (v & 0x0f); }
        void ksrC(int v)    { data[1] = (data[1] & ~0x10) | ((v & 1) << 4); }
        void sustC(int v)   { data[1] = (data[1] & ~0x20) | ((v & 1) << 5); }
        void vibrC(int v)   { data[1] = (data[1] & ~0x40) | ((v & 1) << 6); }
        void tremC(int v)   { data[1] = (data[1] & ~0x80) | ((v & 1) << 7); }
        void volM(int v)    { data[2] = (data[2] & ~0x3f) | (v & 0x3f); }
        void kslM(int v)    { data[2] = (data[2] & ~0xc0) | ((v & 3) << 6); }
        void volC(int v)    { data[3] = (data[3] & ~0x3f) | (v & 0x3f); }
        void kslC(int v)    { data[3] = (data[3] & ~0xc0) | ((v & 3) << 6); }
        void decM(int v)    { data[4] = (data[4] & ~0x0f) | (v & 0x0f); }
        void attckM(int v)  { data[4] = (data[4] & ~0xf0) | ((v & 0x0f) << 4); }
        void decC(int v)    { data[5] = (data[5] & ~0x0f) | (v & 0x0f); }
        void attckC(int v)  { data[5] = (data[5] & ~0xf0) | ((v & 0x0f) << 4); }
        void relM(int v)    { data[6] = (data[6] & ~0x0f) | (v & 0x0f); }
        void sustnM(int v)  { data[6] = (data[6] & ~0xf0) | ((v & 0x0f) << 4); }
        void relC(int v)    { data[7] = (data[7] & ~0x0f) | (v & 0x0f); }
        void sustnC(int v)  { data[7] = (data[7] & ~0xf0) | ((v & 0x0f) << 4); }
        void wformM(int v)  { data[8] = (data[8] & ~0x07) | (v & 0x07); }
        void wformC(int v)  { data[9] = (data[9] & ~0x07) | (v & 0x07); }
        void connect(int v) { data[10] = (data[10] & ~0x01) | (v & 1); }
        void feedb(int v)   { data[10] = (data[10] & ~0x0e) | ((v & 0x07) << 1); }
    }

    /** tINSTR_DATA (14 bytes) */
    private static class InstrData {
        final FmInstData fm = new FmInstData();
        int panning;
        int fineTune; // signed
        int percVoice;

        boolean isEmpty() {
            for (int i = 0; i < 11; i++) {
                if (fm.data[i] != 0) return false;
            }
            return panning == 0 && fineTune == 0 && percVoice == 0;
        }
    }

    /** tREGISTER_TABLE_DEF (15 bytes) */
    private static class RegTableDef {
        final FmInstData fm = new FmInstData();
        int freqSlide; // signed 16-bit
        int panning;
        int duration;
    }

    /** tFMREG_TABLE (3831 bytes) */
    private static class FmregTable {
        int length;
        int loopBegin;
        int loopLength;
        int keyoffPos;
        int arpeggioTable;
        int vibratoTable;
        final RegTableDef[] data = new RegTableDef[255];
    }

    /** tARPEGGIO_TABLE (260 bytes) */
    private static class ArpeggioTable {
        int length;
        int speed;
        int loopBegin;
        int loopLength;
        int keyoffPos;
        final int[] data = new int[255];
    }

    /** tVIBRATO_TABLE (261 bytes) */
    private static class VibratoTable {
        int length;
        int speed;
        int delay;
        int loopBegin;
        int loopLength;
        int keyoffPos;
        final int[] data = new int[255]; // signed
    }

    /** tADTRACK2_EVENT (6 bytes) */
    private static class A2Event {
        int note;
        int instrDef;
        final int[] effDef = new int[2];
        final int[] effVal = new int[2];

        void copyFrom(A2Event o) {
            note = o.note;
            instrDef = o.instrDef;
            effDef[0] = o.effDef[0];
            effDef[1] = o.effDef[1];
            effVal[0] = o.effVal[0];
            effVal[1] = o.effVal[1];
        }
    }

    /** tINSTR_DATA_EXT */
    private static class InstrExt {
        final InstrData instrData = new InstrData();
        int vibrato;
        int arpeggio;
        FmregTable fmreg;
        long disFmregCols;
    }

    private static class MacroTable {
        int fmregPos;
        int arpgPos;
        int vibPos;
        int fmregDuration;
        int arpgCount;
        int vibCount;
        int vibDelay;
        int fmregIns;
        int arpgTable;
        int vibTable;
        int arpgNote;
        boolean vibPaused;
        int vibFreq;
    }

    // ---- song info

    private final int[] patternOrder = new int[0x80];
    private int songTempo;
    private int songSpeed;
    private int commonFlag;
    private int pattLen;
    private int nmTracks;
    private int songMacroSpeedup;
    private int flag4op;
    private final int[] lockFlags = new int[20];

    // ---- instruments / macro tables / events

    private InstrExt[] instruments = new InstrExt[0];
    private VibratoTable[] vibratoTables = new VibratoTable[0];
    private ArpeggioTable[] arpeggioTables = new ArpeggioTable[0];

    private int evPatterns, evRows, evChannels;
    private A2Event[] events = new A2Event[0];
    private final A2Event nullEvent = new A2Event();

    // ---- channel state (tCHDATA)

    private final FmInstData[] fmparTable = new FmInstData[20];
    private final boolean[] volumeLock = new boolean[20];
    private final boolean[] vol4opLock = new boolean[20];
    private final boolean[] peakLock = new boolean[20];
    private final boolean[] panLock = new boolean[20];
    private final int[] modulatorVol = new int[20];
    private final int[] carrierVol = new int[20];
    private final A2Event[] eventTable = new A2Event[20];
    private final int[] voiceTable = new int[20];
    private final int[] freqTable = new int[20];
    private final int[] zeroFqTable = new int[20];
    private final int[][] effectTableDef = new int[2][20];
    private final int[][] effectTableVal = new int[2][20];
    private final int[][] fslideTable = new int[2][20];
    private final int[][] glfsldTableDef = new int[2][20];
    private final int[][] glfsldTableVal = new int[2][20];
    private final int[][] portaTableFreq = new int[2][20];
    private final int[][] portaTableSpeed = new int[2][20];
    private final boolean[] portaFKTable = new boolean[20];
    private final int[][] arpggTableState = new int[2][20];
    private final int[][] arpggTableNote = new int[2][20];
    private final int[][] arpggTableAdd1 = new int[2][20];
    private final int[][] arpggTableAdd2 = new int[2][20];
    private final int[][] vibrTablePos = new int[2][20];
    private final int[][] vibrTableSpeed = new int[2][20];
    private final int[][] vibrTableDepth = new int[2][20];
    private final boolean[][] vibrTableFine = new boolean[2][20];
    private final int[][] tremTablePos = new int[2][20];
    private final int[][] tremTableSpeed = new int[2][20];
    private final int[][] tremTableDepth = new int[2][20];
    private final boolean[][] tremTableFine = new boolean[2][20];
    private final int[][] retrigTable = new int[2][20];
    private final int[][] tremorTablePos = new int[2][20]; // signed
    private final int[][] tremorTableVolM = new int[2][20];
    private final int[][] tremorTableVolC = new int[2][20];
    private final int[] panningTable = new int[20];
    private final int[][] lastEffectDef = new int[2][20];
    private final int[][] lastEffectVal = new int[2][20];
    private final int[] volslideType = new int[20];
    private final int[] notedelTable = new int[20];
    private final int[] notecutTable = new int[20];
    private final int[] ftuneTable = new int[20]; // signed
    private final boolean[] keyoffLoop = new boolean[20];
    private final int[] loopbckTable = new int[20];
    private final int[][] loopTable = new int[20][256];
    private final boolean[] resetChan = new boolean[20];
    private final MacroTable[] macroTable = new MacroTable[20];

    // ---- player state

    private int currentOrder;
    private int currentPattern;
    private int currentLine;

    private int tempo = 50;
    private int speed = 6;

    private int macroSpeedup = 1;
    private boolean irqMode;

    private int IRQ_freq = 50;
    private int IRQ_freq_shift = 0;
    private boolean timerFix = true;

    private boolean patternBreak;
    private boolean patternDelay;
    private int nextLine;

    private int playbackSpeedShift;
    private int overallVolume = 63;
    private int globalVolume = 63;

    private static final int def_vibtrem_speed_factor = 1;
    private static final int def_vibtrem_table_size = 32;

    private int vibtremSpeedFactor = def_vibtrem_speed_factor;
    private int vibtremTableSize = def_vibtrem_table_size;
    private final int[] vibtremTable = new int[256];

    private int miscRegister;

    private int currentTremoloDepth;
    private int currentVibratoDepth;

    private boolean speedUpdate, lockvol, panlock, lockVP;
    private int tremoloDepth, vibratoDepth;
    private boolean volumeScaling, percussionMode;

    // timer
    private int ticks, tickD, tickXF;
    private int ticklooper, macroTicklooper;

    // loader
    /** 0 - a2m, 1 - a2t */
    private int fileType;
    private int ffver = 1;
    private final int[] len = new int[21];
    private final boolean[] adsrCarrier = new boolean[9];

    private boolean songend;

    public A2mV2Player() {
        for (int i = 0; i < 20; i++) {
            fmparTable[i] = new FmInstData();
            eventTable[i] = new A2Event();
            macroTable[i] = new MacroTable();
        }
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("Adlib Tracker 2", "a2m");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("A2M-V2");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            URI uri = SoundUtil.getSource(bitStream);
            if (uri != null) {
                String path = uri.getPath();
                if (path != null) {
                    String lower = path.toLowerCase();
                    if (!lower.endsWith(".a2m") && !lower.endsWith(".a2t")) {
                        return false;
                    }
                }
            }
            bitStream.mark(21);
            byte[] hdr = new byte[21];
            int r = bitStream.read(hdr);
            if (r < 16) return false;
            String id10 = new String(hdr, 0, 10, java.nio.charset.StandardCharsets.US_ASCII);
            if (id10.equals("_A2module_")) {
                int ver = hdr[14] & 0xff;
                return ver >= 1 && ver <= 14;
            }
            if (r >= 21) {
                String id15 = new String(hdr, 0, 15, java.nio.charset.StandardCharsets.US_ASCII);
                if (id15.equals("_A2tiny_module_")) {
                    int ver = hdr[19] & 0xff;
                    return ver >= 1 && ver <= 14;
                }
            }
            return false;
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
        byte[] tune = is.readAllBytes();

        // the framework reuses player instances; emulate adplug's
        // fresh-object-per-file semantics (init_player reads freq_table
        // via key_off before init_buffers clears it)
        resetPlayerState();

        if (!a2Import(tune)) {
            throw new IllegalArgumentException("not an Adlib Tracker 2 module");
        }

        rewind(0);
    }

    /** resets all playback state to constructor defaults */
    private void resetPlayerState() {
        currentOrder = 0;
        currentPattern = 0;
        currentLine = 0;
        tempo = 50;
        speed = 6;
        macroSpeedup = 1;
        irqMode = false;
        IRQ_freq = 50;
        IRQ_freq_shift = 0;
        patternBreak = false;
        patternDelay = false;
        nextLine = 0;
        playbackSpeedShift = 0;
        overallVolume = 63;
        globalVolume = 63;
        miscRegister = 0;
        currentTremoloDepth = 0;
        currentVibratoDepth = 0;
        ticks = 0;
        tickD = 0;
        tickXF = 0;
        ticklooper = 0;
        macroTicklooper = 0;
        songend = false;

        lockvol = false;
        panlock = false;
        lockVP = false;
        Arrays.fill(lockFlags, 0);

        initBuffers();
    }

    @Override
    public float getRefresh() {
        return (float) tempo * macroSpeedupEff();
    }

    @Override
    public boolean update() {
        newtimer();

        return !songend;
    }

    // ---- helpers for instruments

    private InstrExt getInstr(int ins) {
        if (ins == 0 || ins > instruments.length) {
            return null;
        }
        return instruments[ins - 1];
    }

    private int getInstrFineTune(int ins) {
        InstrExt instrument = getInstr(ins);
        return instrument != null ? instrument.instrData.fineTune : 0;
    }

    private static final InstrData zeroins = new InstrData();

    private InstrData getInstrDataByCh(int chan) {
        InstrExt instrument = getInstr(voiceTable[chan]);
        return instrument != null ? instrument.instrData : null;
    }

    private InstrData getInstrData(int ins) {
        InstrExt instrument = getInstr(ins);
        return instrument != null ? instrument.instrData : null;
    }

    // ---- helpers for macro tables

    private ArpeggioTable getArpeggioTable(int arpTable) {
        return arpTable != 0 && arpTable <= arpeggioTables.length ? arpeggioTables[arpTable - 1] : null;
    }

    private VibratoTable getVibratoTable(int vibTable) {
        return vibTable != 0 && vibTable <= vibratoTables.length ? vibratoTables[vibTable - 1] : null;
    }

    private FmregTable getFmregTable(int fmregIns) {
        InstrExt instrument = getInstr(fmregIns);
        return instrument != null ? instrument.fmreg : null;
    }

    // ---- helpers for patterns

    private A2Event getEventP(int pattern, int channel, int row) {
        return pattern < evPatterns
                ? events[pattern * evChannels * evRows + channel * evRows + row]
                : nullEvent;
    }

    private void patternsAllocate(int patterns, int channels, int rows) {
        events = new A2Event[patterns * channels * rows];
        for (int i = 0; i < events.length; i++) {
            events[i] = new A2Event();
        }
        evPatterns = patterns;
        evChannels = channels;
        evRows = rows;
    }

    // ---- register offset tables

    private static boolean noteInRange(int note) {
        return ((note & 0x7f) > 0) && ((note & 0x7f) < 12 * 8 + 1);
    }

    private static final int[][] _ch_n = {
        {   // mm
            0x003, 0x000, 0x004, 0x001, 0x005, 0x002, 0x006, 0x007, 0x008, 0x103,
            0x100, 0x104, 0x101, 0x105, 0x102, 0x106, 0x107, 0x108, BYTE_NULL, BYTE_NULL
        }, { // pm
            0x003, 0x000, 0x004, 0x001, 0x005, 0x002, 0x106, 0x107, 0x108, 0x103,
            0x100, 0x104, 0x101, 0x105, 0x102, 0x006, 0x007, 0x008, 0x008, 0x007
        }
    };

    private static final int[][] _ch_m = {
        {   // mm
            0x008, 0x000, 0x009, 0x001, 0x00a, 0x002, 0x010, 0x011, 0x012, 0x108,
            0x100, 0x109, 0x101, 0x10a, 0x102, 0x110, 0x111, 0x112, BYTE_NULL, BYTE_NULL
        }, { // pm
            0x008, 0x000, 0x009, 0x001, 0x00a, 0x002, 0x110, 0x111, 0x112, 0x108,
            0x100, 0x109, 0x101, 0x10a, 0x102, 0x010, 0x014, 0x012, 0x015, 0x011
        }
    };

    private static final int[][] _ch_c = {
        {
            0x00b, 0x003, 0x00c, 0x004, 0x00d, 0x005, 0x013, 0x014, 0x015, 0x10b,
            0x103, 0x10c, 0x104, 0x10d, 0x105, 0x113, 0x114, 0x115, BYTE_NULL, BYTE_NULL
        }, {
            0x00b, 0x003, 0x00c, 0x004, 0x00d, 0x005, 0x113, 0x114, 0x115, 0x10b,
            0x103, 0x10c, 0x104, 0x10d, 0x105, 0x013, BYTE_NULL, BYTE_NULL, BYTE_NULL, BYTE_NULL
        }
    };

    private int regoffsN(int chan) {
        return _ch_n[percussionMode ? 1 : 0][chan];
    }

    private int regoffsM(int chan) {
        return _ch_m[percussionMode ? 1 : 0][chan];
    }

    private int regoffsC(int chan) {
        return _ch_c[percussionMode ? 1 : 0][chan];
    }

    // ---- OPL output

    private void opl2out(int reg, int data) {
        write(0, reg & 0xff, data & 0xff);
    }

    private void opl3out(int reg, int data) {
        write(reg < 0x100 ? 0 : 1, reg & 0xff, data & 0xff);
    }

    private void opl3exp(int data) {
        write(1, data & 0xff, (data >> 8) & 0xff);
    }

    private static final int[] Fnum = {
        0x156, 0x16b, 0x181, 0x198, 0x1b0, 0x1ca, 0x1e5,
        0x202, 0x220, 0x241, 0x263, 0x287, 0x2ae
    };

    private static int nFreq(int note) {
        note &= 0xff;
        if (note >= 12 * 8) {
            return (7 << 10) | FreqEnd;
        }
        return (note / 12 << 10) | Fnum[note % 12];
    }

    private static int calcFreqShiftUp(int freq, int shift) {
        int oc = (freq >> 10) & 7;
        int fr = (short) ((freq & 0x3ff) + shift);

        if (fr > FreqEnd) {
            if (oc == 7) {
                fr = FreqEnd;
            } else {
                oc++;
                fr -= FreqRange;
            }
        }

        return ((oc << 10) | fr) & 0xffff;
    }

    private static int calcFreqShiftDown(int freq, int shift) {
        int oc = (freq >> 10) & 7;
        int fr = (short) ((freq & 0x3ff) - shift);

        if (fr < FreqStart) {
            if (oc == 0) {
                fr = FreqStart;
            } else {
                oc--;
                fr += FreqRange;
            }
        }

        return ((oc << 10) | fr) & 0xffff;
    }

    private static final int[] vibr = {
        0, 24, 49, 74, 97, 120, 141, 161, 180, 197, 212, 224, 235, 244, 250, 253, 255,
        253, 250, 244, 235, 224, 212, 197, 180, 161, 141, 120, 97, 74, 49, 24
    };

    /** == calc_vibtrem_shift() in AT2 */
    private static int calcVibratoShift(int depth, int position) {
        return (vibr[position & 0x1f] * depth) >> 6;
    }

    /** adplug's max(): clamps value to a maximum */
    private static int clampMax(int value, int maximum) {
        return value > maximum ? maximum : value;
    }

    // ---- little-endian byte helpers

    private static int u8(byte[] b, int off) {
        return off >= 0 && off < b.length ? b[off] & 0xff : 0;
    }

    private static int s8(byte[] b, int off) {
        return off >= 0 && off < b.length ? b[off] : 0;
    }

    private static int u16(byte[] b, int off) {
        return u8(b, off) | (u8(b, off + 1) << 8);
    }

    private static int s16(byte[] b, int off) {
        return (short) u16(b, off);
    }

    private static long u32(byte[] b, int off) {
        return (u8(b, off) & 0xffL) | ((long) u8(b, off + 1) << 8) | ((long) u8(b, off + 2) << 16) | ((long) u8(b, off + 3) << 24);
    }

    // ================================================================
    // LOADER
    // ================================================================

    private void a2tDepack(byte[] src, int srcOff, int srcsize, byte[] dst, int dstsize) {
        switch (ffver) {
        case 1:
        case 5: { // sixpack
            int[] words = new int[srcsize / 2 + 1];
            for (int i = 0; i < srcsize / 2; i++) {
                words[i] = u16(src, srcOff + i * 2);
            }
            Sixdepak.decode(words, srcsize, dst, dstsize);
            break;
        }
        case 2:
        case 6: // lzw
            A2mV2Depack.lzwDecompress(src, srcOff, srcsize, dst, dstsize);
            break;
        case 3:
        case 7: // lzss
            A2mV2Depack.lzssDecompress(src, srcOff, srcsize, dst, dstsize);
            break;
        case 4:
        case 8: // unpacked
            if (dstsize <= srcsize) {
                System.arraycopy(src, srcOff, dst, 0, Math.min(srcsize, dst.length));
            }
            break;
        case 9:
        case 10:
        case 11: // apack (aPlib)
            A2mV2Depack.apDepack(src, srcOff, srcsize, dst, dstsize);
            break;
        case 12:
        case 13:
        case 14: // lzh
            A2mV2Depack.lzhDecompress(src, srcOff, srcsize, dst, dstsize);
            break;
        }
    }

    private void instrumentsAllocate(int number) {
        instruments = new InstrExt[number];
        for (int i = 0; i < number; i++) {
            instruments[i] = new InstrExt();
        }
    }

    /** parses an 11-byte FM block */
    private static void parseFm(FmInstData fm, byte[] b, int off) {
        for (int i = 0; i < 11; i++) {
            fm.data[i] = u8(b, off + i);
        }
    }

    private void instrumentImportV1_8(int ins, byte[] b, int off) {
        InstrData d = getInstrData(ins);
        parseFm(d.fm, b, off);
        d.panning = u8(b, off + 11);
        d.fineTune = s8(b, off + 12);
        if (d.panning >= 3) {
            d.panning = 0;
        }
    }

    private void instrumentImport(int ins, byte[] b, int off) {
        InstrData d = getInstrData(ins);
        parseFm(d.fm, b, off);
        d.panning = u8(b, off + 11);
        d.fineTune = s8(b, off + 12);
        d.percVoice = u8(b, off + 13);
        if (d.panning >= 3) {
            d.panning = 0;
        }
    }

    private static boolean isDataEmpty(byte[] b, int off, int size) {
        for (int i = 0; i < size; i++) {
            if (u8(b, off + i) != 0) return false;
        }
        return true;
    }

    /** parses tFMREG_TABLE (3831 bytes); only allocated when length != 0 */
    private static FmregTable parseFmregTable(byte[] b, int off) {
        FmregTable t = new FmregTable();
        t.length = u8(b, off);
        t.loopBegin = u8(b, off + 1);
        t.loopLength = u8(b, off + 2);
        t.keyoffPos = u8(b, off + 3);
        t.arpeggioTable = u8(b, off + 4);
        t.vibratoTable = u8(b, off + 5);
        for (int i = 0; i < 255; i++) {
            RegTableDef d = new RegTableDef();
            int o = off + 6 + i * 15;
            parseFm(d.fm, b, o);
            d.freqSlide = s16(b, o + 11);
            d.panning = u8(b, o + 13);
            d.duration = u8(b, o + 14);
            t.data[i] = d;
        }
        return t;
    }

    /** allocates fmreg tables on instruments (from an array of raw 3831-byte tables) */
    private void fmregTableAllocate(int n, byte[] b, int off) {
        for (int i = 0; i < n; i++) {
            int length = u8(b, off + i * 3831);
            if (length != 0) {
                InstrExt instrument = getInstr(i + 1);
                if (instrument == null) continue;
                instrument.fmreg = parseFmregTable(b, off + i * 3831);
            }
        }
    }

    /** shrink bool[255][28] to bits */
    private void disabledFmregsImport(int n, byte[] b, int off) {
        for (int i = 0; i < n; i++) {
            long result = 0;
            for (int bit = 0; bit < 28; bit++) {
                result |= (long) (u8(b, off + i * 28 + bit) & 1) << bit;
            }
            InstrExt instrument = getInstr(i + 1);
            if (instrument == null) continue;
            instrument.disFmregCols = result;
        }
    }

    /** allocates arpeggio/vibrato tables (from an array of raw 521-byte tARPVIB_TABLE) */
    private void arpvibTablesAllocate(int n, byte[] b, int off) {
        vibratoTables = new VibratoTable[n];
        arpeggioTables = new ArpeggioTable[n];

        for (int i = 0; i < n; i++) {
            int o = off + i * 521;
            // arpeggio (260 bytes)
            int arpLength = u8(b, o);
            if (arpLength != 0) {
                ArpeggioTable at = new ArpeggioTable();
                at.length = arpLength;
                at.speed = u8(b, o + 1);
                at.loopBegin = u8(b, o + 2);
                at.loopLength = u8(b, o + 3);
                at.keyoffPos = u8(b, o + 4);
                for (int k = 0; k < 255; k++) {
                    at.data[k] = u8(b, o + 5 + k);
                }
                arpeggioTables[i] = at;
            }
            // vibrato (261 bytes)
            int vo = o + 260;
            int vibLength = u8(b, vo);
            if (vibLength != 0) {
                VibratoTable vt = new VibratoTable();
                vt.length = vibLength;
                vt.speed = u8(b, vo + 1);
                vt.delay = u8(b, vo + 2);
                vt.loopBegin = u8(b, vo + 3);
                vt.loopLength = u8(b, vo + 4);
                vt.keyoffPos = u8(b, vo + 5);
                for (int k = 0; k < 255; k++) {
                    vt.data[k] = s8(b, vo + 6 + k);
                }
                vibratoTables[i] = vt;
            }
        }
    }

    private static final int SIZEOF_INSTR_V1_8 = 13;
    private static final int SIZEOF_INSTR = 14;
    private static final int SIZEOF_FMREG = 3831;
    private static final int SIZEOF_ARPVIB = 521;
    private static final int SIZEOF_BPM_DATA = 3;
    private static final int SIZEOF_INS_4OP_FLAGS = 129;
    private static final int SIZEOF_RESERVED = 1024;

    private int a2tReadVarheader(byte[] tune, int off, int size) {
        switch (ffver) {
        case 1: case 2: case 3: case 4:
            if (12 > size) return -1;
            for (int i = 0; i < 6; i++) {
                len[i] = u16(tune, off + i * 2);
            }
            return 12;
        case 5: case 6: case 7: case 8:
            if (21 > size) return -1;
            commonFlag = u8(tune, off);
            for (int i = 0; i < 10; i++) {
                len[i] = u16(tune, off + 1 + i * 2);
            }
            return 21;
        case 9:
            if (86 > size) return -1;
            commonFlag = u8(tune, off);
            pattLen = u16(tune, off + 1);
            nmTracks = u8(tune, off + 3);
            songMacroSpeedup = u16(tune, off + 4);
            for (int i = 0; i < 20; i++) {
                len[i] = (int) u32(tune, off + 6 + i * 4);
            }
            return 86;
        case 10:
            if (107 > size) return -1;
            commonFlag = u8(tune, off);
            pattLen = u16(tune, off + 1);
            nmTracks = u8(tune, off + 3);
            songMacroSpeedup = u16(tune, off + 4);
            flag4op = u8(tune, off + 6);
            for (int i = 0; i < 20; i++) {
                lockFlags[i] = u8(tune, off + 7 + i);
            }
            for (int i = 0; i < 20; i++) {
                len[i] = (int) u32(tune, off + 27 + i * 4);
            }
            return 107;
        case 11: case 12: case 13: case 14:
            if (111 > size) return -1;
            commonFlag = u8(tune, off);
            pattLen = u16(tune, off + 1);
            nmTracks = u8(tune, off + 3);
            songMacroSpeedup = u16(tune, off + 4);
            flag4op = u8(tune, off + 6);
            for (int i = 0; i < 20; i++) {
                lockFlags[i] = u8(tune, off + 7 + i);
            }
            for (int i = 0; i < 21; i++) {
                len[i] = (int) u32(tune, off + 27 + i * 4);
            }
            return 111;
        }

        return -1;
    }

    private int a2tReadInstruments(byte[] tune, int off, int size) {
        if (len[0] > size) return -1;

        int instnum = ffver < 9 ? 250 : 255;
        int instsize = ffver < 9 ? SIZEOF_INSTR_V1_8 : SIZEOF_INSTR;
        int dstsize = (instnum * instsize) +
                (ffver > 11 ? SIZEOF_BPM_DATA + SIZEOF_INS_4OP_FLAGS + SIZEOF_RESERVED : 0);
        byte[] dst = new byte[dstsize];

        a2tDepack(tune, off, len[0], dst, dstsize);

        int dstOff = 0;
        if (ffver == 14) {
            dstOff += SIZEOF_BPM_DATA;
        }
        if (ffver >= 12 && ffver <= 14) {
            dstOff += SIZEOF_INS_4OP_FLAGS;
            dstOff += SIZEOF_RESERVED;
        }

        // calculate the real number of used instruments
        int count = instnum;
        while (count > 0 && isDataEmpty(dst, dstOff + (count - 1) * instsize, instsize)) {
            count--;
        }

        instrumentsAllocate(count);

        if (ffver < 9) {
            for (int i = 0; i < count; i++) {
                instrumentImportV1_8(i + 1, dst, dstOff + i * instsize);
            }
        } else {
            for (int i = 0; i < count; i++) {
                instrumentImport(i + 1, dst, dstOff + i * instsize);
            }
        }

        return len[0];
    }

    private int a2tReadFmregtable(byte[] tune, int off, int size) {
        if (ffver < 9) return 0;

        if (len[1] > size) return -1;

        byte[] data = new byte[255 * SIZEOF_FMREG];
        a2tDepack(tune, off, len[1], data, data.length);

        int count = instruments.length;

        fmregTableAllocate(count, data, 0);

        for (int i = 0; i < count; i++) {
            InstrExt dst = getInstr(i + 1);
            dst.arpeggio = u8(data, i * SIZEOF_FMREG + 4);
            dst.vibrato = u8(data, i * SIZEOF_FMREG + 5);
        }

        return len[1];
    }

    private int a2tReadArpvibtable(byte[] tune, int off, int size) {
        if (ffver < 9) return 0;

        if (len[2] > size) return -1;

        byte[] data = new byte[255 * SIZEOF_ARPVIB];
        a2tDepack(tune, off, len[2], data, data.length);

        arpvibTablesAllocate(255, data, 0);

        return len[2];
    }

    private int a2tReadDisabledFmregs(byte[] tune, int off, int size) {
        if (ffver < 11) return 0;

        if (len[3] > size) return -1;

        byte[] data = new byte[255 * 28];
        a2tDepack(tune, off, len[3], data, data.length);

        disabledFmregsImport(instruments.length, data, 0);

        return len[3];
    }

    private int a2tReadOrder(byte[] tune, int off, int size) {
        int[] blocknum = { 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 4, 4, 4, 4 };
        int i = blocknum[ffver - 1];

        if (len[i] > size) return -1;

        byte[] order = new byte[128];
        a2tDepack(tune, off, len[i], order, order.length);
        for (int k = 0; k < 128; k++) {
            patternOrder[k] = order[k] & 0xff;
        }

        return len[i];
    }

    /** mutable event during v1-4 conversion */
    private static class OldEvent {
        int note, instrDef, effectDef, effect;
    }

    private void convertV1234Event(OldEvent ev, int chan) {
        switch (ev.effectDef) {
        case fx_Arpeggio:        ev.effectDef = ef_Arpeggio;        break;
        case fx_FSlideUp:        ev.effectDef = ef_FSlideUp;        break;
        case fx_FSlideDown:      ev.effectDef = ef_FSlideDown;      break;
        case fx_FSlideUpFine:    ev.effectDef = ef_FSlideUpFine;    break;
        case fx_FSlideDownFine:  ev.effectDef = ef_FSlideDownFine;  break;
        case fx_TonePortamento:  ev.effectDef = ef_TonePortamento;  break;
        case fx_TPortamVolSlide: ev.effectDef = ef_TPortamVolSlide; break;
        case fx_Vibrato:         ev.effectDef = ef_Vibrato;         break;
        case fx_VibratoVolSlide: ev.effectDef = ef_VibratoVolSlide; break;
        case fx_SetInsVolume:    ev.effectDef = ef_SetInsVolume;    break;
        case fx_PatternJump:     ev.effectDef = ef_PositionJump;    break;
        case fx_PatternBreak:    ev.effectDef = ef_PatternBreak;    break;
        case fx_SetTempo:        ev.effectDef = ef_SetSpeed;        break;
        case fx_SetTimer:        ev.effectDef = ef_SetTempo;        break;
        case fx_SetOpIntensity:
            if ((ev.effect & 0xf0) != 0) {
                ev.effectDef = ef_SetCarrierVol;
                ev.effect = (ev.effect >> 4) * 4 + 3;
            } else if ((ev.effect & 0x0f) != 0) {
                ev.effectDef = ef_SetModulatorVol;
                ev.effect = (ev.effect & 0x0f) * 4 + 3;
            } else {
                ev.effectDef = 0;
            }
            break;
        case fx_Extended:
            switch (ev.effect >> 4) {
            case fx_ex_DefAMdepth:
                ev.effectDef = ef_Extended;
                ev.effect = ef_ex_SetTremDepth << 4 | (ev.effect & 0x0f);
                break;
            case fx_ex_DefVibDepth:
                ev.effectDef = ef_Extended;
                ev.effect = ef_ex_SetVibDepth << 4 | (ev.effect & 0x0f);
                break;
            case fx_ex_DefWaveform:
                ev.effectDef = ef_SetWaveform;
                if ((ev.effect & 0x0f) < 4) {
                    ev.effect = ((ev.effect & 0x0f) << 4) | 0x0f; // 0..3
                } else {
                    ev.effect = ((ev.effect & 0x0f) - 4) | 0xf0; // 4..7
                }
                break;
            case fx_ex_VSlideUp:
                ev.effectDef = ef_VolSlide;
                ev.effect = (ev.effect & 0x0f) << 4;
                break;
            case fx_ex_VSlideDown:
                ev.effectDef = ef_VolSlide;
                ev.effect = ev.effect & 0x0f;
                break;
            case fx_ex_VSlideUpFine:
                ev.effectDef = ef_VolSlideFine;
                ev.effect = (ev.effect & 0x0f) << 4;
                break;
            case fx_ex_VSlideDownFine:
                ev.effectDef = ef_VolSlideFine;
                ev.effect = ev.effect & 0x0f;
                break;
            case fx_ex_ManSlideUp:
                ev.effectDef = ef_Extended2;
                ev.effect = (ef_ex2_FineTuneUp << 4) | (ev.effect & 0x0f);
                break;
            case fx_ex_ManSlideDown:
                ev.effectDef = ef_Extended2;
                ev.effect = (ef_ex2_FineTuneDown << 4) | (ev.effect & 0x0f);
                break;
            case fx_ex_RetrigNote:
                ev.effectDef = ef_RetrigNote;
                ev.effect = (ev.effect & 0x0f) + 1;
                break;
            case fx_ex_SetAttckRate:
                ev.effectDef = ef_Extended;
                ev.effect = ev.effect & 0x0f;
                if (!adsrCarrier[chan]) {
                    ev.effect |= ef_ex_SetAttckRateM << 4;
                } else {
                    ev.effect |= ef_ex_SetAttckRateC << 4;
                }
                break;
            case fx_ex_SetDecayRate:
                ev.effectDef = ef_Extended;
                ev.effect = ev.effect & 0x0f;
                if (!adsrCarrier[chan]) {
                    ev.effect |= ef_ex_SetDecayRateM << 4;
                } else {
                    ev.effect |= ef_ex_SetDecayRateC << 4;
                }
                break;
            case fx_ex_SetSustnLevel:
                ev.effectDef = ef_Extended;
                ev.effect = ev.effect & 0x0f;
                if (!adsrCarrier[chan]) {
                    ev.effect |= ef_ex_SetSustnLevelM << 4;
                } else {
                    ev.effect |= ef_ex_SetSustnLevelC << 4;
                }
                break;
            case fx_ex_SetReleaseRate:
                ev.effectDef = ef_Extended;
                ev.effect = ev.effect & 0x0f;
                if (!adsrCarrier[chan]) {
                    ev.effect |= ef_ex_SetRelRateM << 4;
                } else {
                    ev.effect |= ef_ex_SetRelRateC << 4;
                }
                break;
            case fx_ex_SetFeedback:
                ev.effectDef = ef_Extended;
                ev.effect = (ef_ex_SetFeedback << 4) | (ev.effect & 0x0f);
                break;
            case fx_ex_ExtendedCmd:
                ev.effectDef = ef_Extended;
                ev.effect = ef_ex_ExtendedCmd2 << 4;
                if ((ev.effect & 0x0f) < 10) {
                    switch (ev.effect & 0x0f) {
                    case 0: ev.effect |= ef_ex_cmd2_RSS;       break;
                    case 1: ev.effect |= ef_ex_cmd2_LockVol;   break;
                    case 2: ev.effect |= ef_ex_cmd2_UnlockVol; break;
                    case 3: ev.effect |= ef_ex_cmd2_LockVP;    break;
                    case 4: ev.effect |= ef_ex_cmd2_UnlockVP;  break;
                    case 5:
                        ev.effectDef = 0;
                        ev.effect = 0;
                        adsrCarrier[chan] = true;
                        break;
                    case 6:
                        ev.effectDef = 0;
                        ev.effect = 0;
                        adsrCarrier[chan] = false;
                        break;
                    case 7: ev.effect |= ef_ex_cmd2_VSlide_car; break;
                    case 8: ev.effect |= ef_ex_cmd2_VSlide_mod; break;
                    case 9: ev.effect |= ef_ex_cmd2_VSlide_def; break;
                    }
                } else {
                    ev.effectDef = 0;
                    ev.effect = 0;
                }
                break;
            }
            break;
        default:
            ev.effectDef = 0;
            ev.effect = 0;
        }
    }

    /** common for both a2t/a2m; s is the starting block index */
    private int a2ReadPatterns(byte[] tune, int off, int s, int size) {
        int retval = 0;
        switch (ffver) {
        case 1: case 2: case 3: case 4: { // [4][16][64][9][4]
            byte[] old = new byte[16 * 2304];
            OldEvent ev = new OldEvent();

            Arrays.fill(adsrCarrier, false);

            for (int i = 0; i < 4; i++) {
                if (len[i + s] == 0) continue;

                if (len[i + s] > size) return -1;

                Arrays.fill(old, (byte) 0);
                a2tDepack(tune, off, len[i + s], old, old.length);

                for (int p = 0; p < 16; p++) { // pattern
                    if (i * 8 + p >= evPatterns) break;
                    for (int r = 0; r < 64; r++) { // row
                        for (int c = 0; c < 9; c++) { // channel
                            int o = p * 2304 + r * 36 + c * 4;
                            ev.note = u8(old, o);
                            ev.instrDef = u8(old, o + 1);
                            ev.effectDef = u8(old, o + 2);
                            ev.effect = u8(old, o + 3);

                            convertV1234Event(ev, c);

                            A2Event dst = getEventP(i * 16 + p, c, r);
                            dst.note = ev.note;
                            dst.instrDef = ev.instrDef;
                            dst.effDef[0] = ev.effectDef;
                            dst.effVal[0] = ev.effect;
                        }
                    }
                }

                off += len[i + s];
                size -= len[i + s];
                retval += len[i + s];
            }
            break;
        }
        case 5: case 6: case 7: case 8: { // [8][8][18][64][4]
            byte[] old = new byte[8 * 4608];

            for (int i = 0; i < 8; i++) {
                if (len[i + s] == 0) continue;

                if (len[i + s] > size) return -1;

                Arrays.fill(old, (byte) 0);
                a2tDepack(tune, off, len[i + s], old, old.length);

                for (int p = 0; p < 8; p++) { // pattern
                    if (i * 8 + p >= evPatterns) break;
                    for (int c = 0; c < 18; c++) { // channel
                        for (int r = 0; r < 64; r++) { // row
                            int o = p * 4608 + c * 256 + r * 4;
                            A2Event dst = getEventP(i * 8 + p, c, r);

                            dst.note = u8(old, o);
                            dst.instrDef = u8(old, o + 1);
                            dst.effDef[0] = u8(old, o + 2);
                            dst.effVal[0] = u8(old, o + 3);
                        }
                    }
                }

                off += len[i + s];
                size -= len[i + s];
                retval += len[i + s];
            }
            break;
        }
        case 9: case 10: case 11: case 12: case 13: case 14: { // [16][8][20][256][6]
            byte[] old = new byte[8 * 30720];

            // 16 groups of 8 patterns
            for (int i = 0; i < 16; i++) {
                if (len[i + s] == 0) continue;
                if (len[i + s] > size) return -1;

                Arrays.fill(old, (byte) 0);
                a2tDepack(tune, off, len[i + s], old, old.length);
                off += len[i + s];
                size -= len[i + s];
                retval += len[i + s];

                for (int p = 0; p < 8; p++) { // pattern
                    if (i * 8 + p >= evPatterns) break;

                    for (int c = 0; c < evChannels; c++) { // channel
                        for (int r = 0; r < evRows; r++) { // row
                            int o = p * 30720 + c * 1536 + r * 6;
                            A2Event dst = getEventP(i * 8 + p, c, r);

                            dst.note = u8(old, o);
                            dst.instrDef = u8(old, o + 1);
                            dst.effDef[0] = u8(old, o + 2);
                            dst.effVal[0] = u8(old, o + 3);
                            dst.effDef[1] = u8(old, o + 4);
                            dst.effVal[1] = u8(old, o + 5);
                        }
                    }
                }
            }
            break;
        }
        }

        return retval;
    }

    /** clean songinfo before importing a2t tune */
    private void initSongdata() {
        Arrays.fill(patternOrder, 0x80);
        songTempo = tempo;
        songSpeed = speed;
        commonFlag = 0;
        flag4op = 0;
        Arrays.fill(lockFlags, 0);

        IRQ_freq_shift = 0;
        playbackSpeedShift = 0;
        pattLen = 64;
        nmTracks = 18;
        songMacroSpeedup = 1;
        speedUpdate = false;
        lockvol = false;
        panlock = false;
        lockVP = false;
        tremoloDepth = 0;
        vibratoDepth = 0;
        volumeScaling = false;
        percussionMode = false;
    }

    private boolean a2tImport(byte[] tune) {
        // A2T_HEADER: id(15) crc(4) ffver(1) npatt(1) tempo(1) speed(1) = 23
        if (23 > tune.length) return false;

        if (!new String(tune, 0, 15, java.nio.charset.StandardCharsets.US_ASCII).equals("_A2tiny_module_"))
            return false;

        initSongdata();

        Arrays.fill(len, 0);

        ffver = u8(tune, 19);
        fileType = 1;

        if (ffver == 0 || ffver > 14) return false;

        int npatt = u8(tune, 20);
        songTempo = u8(tune, 21);
        songSpeed = u8(tune, 22);
        pattLen = 64;
        nmTracks = 18;
        songMacroSpeedup = 1;

        int off = 23;

        // read variable part after header, fill len[] with values
        int result = a2tReadVarheader(tune, off, tune.length - off);
        if (result < 0) return false;
        off += result;

        speedUpdate     = ((commonFlag >> 0) & 1) != 0;
        lockvol         = ((commonFlag >> 1) & 1) != 0;
        lockVP          = ((commonFlag >> 2) & 1) != 0;
        tremoloDepth    = (commonFlag >> 3) & 1;
        vibratoDepth    = (commonFlag >> 4) & 1;
        panlock         = ((commonFlag >> 5) & 1) != 0;
        percussionMode  = ((commonFlag >> 6) & 1) != 0;
        volumeScaling   = ((commonFlag >> 7) & 1) != 0;

        // read instruments; all versions
        result = a2tReadInstruments(tune, off, tune.length - off);
        if (result < 0) return false;
        off += result;

        // read instrument macro (v >= 9,10,11)
        result = a2tReadFmregtable(tune, off, tune.length - off);
        if (result < 0) return false;
        off += result;

        // read arpeggio/vibrato macro table (v >= 9,10,11)
        result = a2tReadArpvibtable(tune, off, tune.length - off);
        if (result < 0) return false;
        off += result;

        // read disabled fm regs (v == 11)
        result = a2tReadDisabledFmregs(tune, off, tune.length - off);
        if (result < 0) return false;
        off += result;

        // read pattern_order
        result = a2tReadOrder(tune, off, tune.length - off);
        if (result < 0) return false;
        off += result;

        // allocate patterns
        patternsAllocate(npatt, nmTracks, pattLen);

        // read patterns
        int[] blockstart = { 2, 2, 2, 2, 2, 2, 2, 2, 4, 4, 5, 5, 5, 5 };
        result = a2ReadPatterns(tune, off, blockstart[ffver - 1], tune.length - off);
        return result >= 0;
    }

    private int a2mReadVarheader(byte[] tune, int off, int npatt, int size) {
        int lensize;
        int maxblock = (ffver < 5 ? npatt / 16 : npatt / 8) + 1;

        if (ffver < 5) lensize = 5;      // 1,2,3,4 - uint16_t len[5];
        else if (ffver < 9) lensize = 9; // 5,6,7,8 - uint16_t len[9];
        else lensize = 17;               // 9,10,11 - uint32_t len[17];

        if (ffver >= 1 && ffver <= 8) {
            if (lensize * 2 > size) return -1;

            // skip possible rubbish (MARIO.A2M)
            for (int i = 0; (i < lensize) && (i <= maxblock); i++) {
                len[i] = u16(tune, off + i * 2);
            }

            return lensize * 2;
        } else if (ffver >= 9 && ffver <= 14) {
            if (lensize * 4 > size) return -1;

            for (int i = 0; i < lensize; i++) {
                len[i] = (int) u32(tune, off + i * 4);
            }

            return lensize * 4;
        }

        return -1;
    }

    private int a2mReadSongdata(byte[] tune, int off, int size) {
        if (ffver < 9) { // 1 - 8
            if (len[0] > size) return -1;
            // A2M_SONGDATA_V1_8 (11717 bytes)
            byte[] data = new byte[11717];
            a2tDepack(tune, off, len[0], data, data.length);

            // songname @0, composer @43, instr_names @86, instr_data @8336,
            // pattern_order @11586, tempo @11714, speed @11715, common_flag @11716

            // calculate the real number of used instruments
            int count = 250;
            while (count > 0 && isDataEmpty(data, 8336 + (count - 1) * SIZEOF_INSTR_V1_8, SIZEOF_INSTR_V1_8)) {
                count--;
            }

            instrumentsAllocate(count);

            for (int i = 0; i < count; i++) {
                instrumentImportV1_8(i + 1, data, 8336 + i * SIZEOF_INSTR_V1_8);
            }

            for (int i = 0; i < 128; i++) {
                patternOrder[i] = u8(data, 11586 + i);
            }

            songTempo = u8(data, 11714);
            songSpeed = u8(data, 11715);

            if (ffver > 4) { // 5 - 8
                commonFlag = u8(data, 11716);
            }
        } else { // 9 - 14
            if (len[0] > size) return -1;
            // A2M_SONGDATA_V9_14 (1138338 bytes)
            byte[] data = new byte[1138338];
            a2tDepack(tune, off, len[0], data, data.length);

            // songname @0, composer @43, instr_names @86,
            // instr_data @11051, fmreg_table @14621, arpvib_table @991526,
            // pattern_order @1124381, tempo @1124509, speed @1124510,
            // common_flag @1124511, patt_len @1124512, nm_tracks @1124514,
            // macro_speedup @1124515, flag_4op @1124517, lock_flags @1124518,
            // pattern_names @1124538, dis_fmreg_col @1130042,
            // ins_4op_flags @1137182, reserved @1137311, bpm_data @1138335

            // calculate the real number of used instruments
            int count = 255;
            while (count > 0 && isDataEmpty(data, 11051 + (count - 1) * SIZEOF_INSTR, SIZEOF_INSTR)) {
                count--;
            }

            instrumentsAllocate(count);

            for (int i = 0; i < count; i++) {
                instrumentImport(i + 1, data, 11051 + i * SIZEOF_INSTR);

                // instrument arpeggio/vibrato references
                InstrExt dst = getInstr(i + 1);
                dst.arpeggio = u8(data, 14621 + i * SIZEOF_FMREG + 4);
                dst.vibrato = u8(data, 14621 + i * SIZEOF_FMREG + 5);
            }

            // allocate fmreg macro tables
            fmregTableAllocate(count, data, 14621);

            // allocate arpeggio/vibrato macro tables
            arpvibTablesAllocate(255, data, 991526);

            for (int i = 0; i < 128; i++) {
                patternOrder[i] = u8(data, 1124381 + i);
            }

            songTempo = u8(data, 1124509);
            songSpeed = u8(data, 1124510);
            commonFlag = u8(data, 1124511);
            pattLen = u16(data, 1124512);
            nmTracks = u8(data, 1124514);
            songMacroSpeedup = u16(data, 1124515);

            // v10
            flag4op = u8(data, 1124517);
            for (int i = 0; i < 20; i++) {
                lockFlags[i] = u8(data, 1124518 + i);
            }

            // v11
            disabledFmregsImport(count, data, 1130042);
        }

        speedUpdate     = ((commonFlag >> 0) & 1) != 0;
        lockvol         = ((commonFlag >> 1) & 1) != 0;
        lockVP          = ((commonFlag >> 2) & 1) != 0;
        tremoloDepth    = (commonFlag >> 3) & 1;
        vibratoDepth    = (commonFlag >> 4) & 1;
        panlock         = ((commonFlag >> 5) & 1) != 0;
        percussionMode  = ((commonFlag >> 6) & 1) != 0;
        volumeScaling   = ((commonFlag >> 7) & 1) != 0;

        return len[0];
    }

    private boolean a2mImport(byte[] tune) {
        // A2M_HEADER: id(10) crc(4) ffver(1) npatt(1) = 16
        if (16 > tune.length) return false;

        if (!new String(tune, 0, 10, java.nio.charset.StandardCharsets.US_ASCII).equals("_A2module_"))
            return false;

        initSongdata();
        Arrays.fill(len, 0);

        ffver = u8(tune, 14);
        fileType = 0;

        if (ffver == 0 || ffver > 14) return false;

        int npatt = u8(tune, 15);

        pattLen = 64;
        nmTracks = 18;
        songMacroSpeedup = 1;

        int off = 16;

        // read variable part after header, fill len[] with values
        int result = a2mReadVarheader(tune, off, npatt, tune.length - off);
        if (result < 0) return false;
        off += result;

        // read songdata
        result = a2mReadSongdata(tune, off, tune.length - off);
        if (result < 0) return false;
        off += result;

        // allocate patterns
        patternsAllocate(npatt, nmTracks, pattLen);

        // read patterns
        result = a2ReadPatterns(tune, off, 1, tune.length - off);
        return result >= 0;
    }

    private boolean a2Import(byte[] tune) {
        if (tune.length > 10 && new String(tune, 0, 10, java.nio.charset.StandardCharsets.US_ASCII).equals("_A2module_")) {
            return a2mImport(tune);
        }

        if (tune.length > 15 && new String(tune, 0, 15, java.nio.charset.StandardCharsets.US_ASCII).equals("_A2tiny_module_")) {
            return a2tImport(tune);
        }

        return false;
    }

    // ================================================================
    // PLAYER
    // ================================================================

    @Override
    public void rewind(int subSong) {
        for (int i = 0; i < 256; i++) {
            write(0, i, 0);
            write(1, i, 0);
        }

        initPlayer();

        songend = false;
        setCurrentOrder(0);

        if (patternOrder[currentOrder] > 0x7f) {
            return;
        }

        currentPattern = patternOrder[currentOrder];
        currentLine = 0;
        patternBreak = false;
        patternDelay = false;
        tickXF = 0;
        ticks = 0;
        nextLine = 0;
        irqMode = true;

        ticklooper = 0;
        macroTicklooper = 0;
        speed = songSpeed;
        macroSpeedup = songMacroSpeedup;
        updateTimer(songTempo);
    }

    private int macroSpeedupEff() {
        return macroSpeedup != 0 ? macroSpeedup : 1;
    }

    private void updateTimer(int hz) {
        if (hz == 0) {
            return;
        } else {
            tempo = hz;
        }

        if (tempo == 18 && timerFix) {
            IRQ_freq = (int) (((float) tempo + 0.2) * 20.0);
        } else {
            IRQ_freq = 250;
        }

        while (IRQ_freq % (tempo * macroSpeedupEff()) != 0) {
            IRQ_freq++;
        }

        if (IRQ_freq > MAX_IRQ_FREQ) {
            IRQ_freq = MAX_IRQ_FREQ;
        }

        while ((IRQ_freq + IRQ_freq_shift + playbackSpeedShift > MAX_IRQ_FREQ) && (playbackSpeedShift > 0))
            playbackSpeedShift--;

        while ((IRQ_freq + IRQ_freq_shift + playbackSpeedShift > MAX_IRQ_FREQ) && (IRQ_freq_shift > 0))
            IRQ_freq_shift--;
    }

    private void updatePlaybackSpeed(int speedShift) {
        if (speedShift == 0) return;

        if ((speedShift > 0) && (IRQ_freq + playbackSpeedShift + speedShift > MAX_IRQ_FREQ)) {
            while (IRQ_freq + IRQ_freq_shift + playbackSpeedShift + speedShift > MAX_IRQ_FREQ)
                speedShift--;
        } else if ((speedShift < 0) && (IRQ_freq + IRQ_freq_shift + playbackSpeedShift + speedShift < MIN_IRQ_FREQ)) {
            while (IRQ_freq + IRQ_freq_shift + playbackSpeedShift + speedShift < MIN_IRQ_FREQ)
                speedShift++;
        }

        playbackSpeedShift += speedShift;
        updateTimer(tempo);
    }

    private void changeFreq(int chan, int freq) {
        if (is4opChan(chan) && is4opChanHi(chan)) {
            freqTable[chan + 1] = freqTable[chan];
            chan++;
        }

        freqTable[chan] &= ~0x1fff;
        freqTable[chan] |= (freq & 0x1fff);
        freqTable[chan] &= 0xffff;

        int n = regoffsN(chan);

        opl3out(0xa0 + n, freqTable[chan] & 0xFF);
        opl3out(0xb0 + n, (freqTable[chan] >> 8) & 0xFF);

        if (is4opChan(chan) && is4opChanLo(chan)) {
            freqTable[chan - 1] = freqTable[chan];
        }
    }

    private boolean isChanAdsrDataEmpty(int chan) {
        FmInstData fmpar = fmparTable[chan];

        return fmpar.data[4] == 0 &&
                fmpar.data[5] == 0 &&
                fmpar.data[6] == 0 &&
                fmpar.data[7] == 0;
    }

    private boolean isInsAdsrDataEmpty(int ins) {
        InstrData i = getInstrData(ins);
        if (i == null) i = zeroins;

        return i.fm.data[4] == 0 &&
                i.fm.data[5] == 0 &&
                i.fm.data[6] == 0 &&
                i.fm.data[7] == 0;
    }

    private void changeFrequency(int chan, int freq) {
        macroTable[chan].vibPaused = true;
        changeFreq(chan, freq);

        if (is4opChan(chan)) {
            int i = is4opChanHi(chan) ? 1 : -1;

            macroTable[chan + i].vibCount = 1;
            macroTable[chan + i].vibPos = 0;
            macroTable[chan + i].vibFreq = freq & 0xffff;
            macroTable[chan + i].vibPaused = false;
        }

        macroTable[chan].vibCount = 1;
        macroTable[chan].vibPos = 0;
        macroTable[chan].vibFreq = freq & 0xffff;
        macroTable[chan].vibPaused = false;
    }

    private void keyOn(int chan) {
        int i = is4opChan(chan) && is4opChanHi(chan) ? 1 : 0;

        opl3out(0xb0 + regoffsN(chan + i), 0);
    }

    private void keyOff(int chan) {
        freqTable[chan] &= ~0x2000;
        changeFrequency(chan, freqTable[chan]);
        eventTable[chan].note |= keyoff_flag;
    }

    private void releaseSustainingSound(int chan) {
        int m = regoffsM(chan);
        int c = regoffsC(chan);

        opl3out(0x40 + m, 63);
        opl3out(0x40 + c, 63);

        // clear adsrw_mod and adsrw_car
        for (int i = 4; i <= 9; i++) {
            fmparTable[chan].data[i] = 0;
        }

        keyOn(chan);
        opl3out(0x60 + m, BYTE_NULL);
        opl3out(0x60 + c, BYTE_NULL);
        opl3out(0x80 + m, BYTE_NULL);
        opl3out(0x80 + c, BYTE_NULL);

        keyOff(chan);
        eventTable[chan].instrDef = 0;
        resetChan[chan] = true;
    }

    /** inverted volume here */
    private static int scaleVolume(int volume, int scaleFactor) {
        return 63 - ((63 - volume) * (63 - scaleFactor) / 63);
    }

    private static class FourOpData {
        boolean mode;
        int conn;
        int ch1, ch2;
        int ins1, ins2;
    }

    /** former _4op_data_flag() */
    private FourOpData get4opData(int chan) {
        FourOpData d = new FourOpData();

        if (!is4opChan(chan)) return d;

        d.mode = true;

        if (is4opChanHi(chan)) {
            d.ch1 = chan;
            d.ch2 = chan + 1;
        } else {
            d.ch1 = chan - 1;
            d.ch2 = chan;
        }

        d.ins1 = eventTable[d.ch1].instrDef;
        if (d.ins1 == 0) d.ins1 = voiceTable[d.ch1];

        d.ins2 = eventTable[d.ch2].instrDef;
        if (d.ins2 == 0) d.ins2 = voiceTable[d.ch2];

        if (d.ins1 != 0 && d.ins2 != 0) {
            InstrData i1 = getInstrData(d.ins1);
            InstrData i2 = getInstrData(d.ins2);
            if (i1 == null) i1 = zeroins;
            if (i2 == null) i2 = zeroins;
            d.conn = (i1.fm.connect() << 1) | i2.fm.connect();
        }

        return d;
    }

    private boolean is4opVolValidChan(int chan) {
        FourOpData d = get4opData(chan);

        return d.mode && vol4opLock[chan] && d.ins1 != 0 && d.ins2 != 0;
    }

    /** inverted volume here */
    private void setInsVolume(int modulator, int carrier, int chan) {
        if (chan >= 20) {
            return;
        }

        InstrData instr = getInstrDataByCh(chan);
        if (instr == null) instr = zeroins;

        // ** OPL3 emulation workaround **
        // force muted instrument volume with missing channel ADSR data
        // when there is additionally no FM-reg macro defined for this instrument
        FmregTable fmreg = getFmregTable(voiceTable[chan]);
        int fmregLength = fmreg != null ? fmreg.length : 0;

        if (isChanAdsrDataEmpty(chan) && fmregLength == 0) {
            modulator = 63;
            carrier = 63;
        }

        int m = regoffsM(chan);
        int c = regoffsC(chan);

        // Note: fmpar volM/volC has pure unscaled volume,
        // modulator_vol/carrier_vol have scaled but without overall_volume
        if (modulator != BYTE_NULL) {
            int regm;
            boolean isPercChan = instr.fm.connect() != 0 ||
                    (percussionMode && chan >= 16); // in [17..20]

            fmparTable[chan].volM(modulator);

            if (isPercChan) { // in [17..20]
                if (volumeScaling) {
                    modulator = scaleVolume(instr.fm.volM(), modulator);
                }

                modulator = scaleVolume(modulator, 63 - globalVolume);
                regm = scaleVolume(modulator, 63 - overallVolume) + (fmparTable[chan].kslM() << 6);
            } else {
                regm = modulator + (fmparTable[chan].kslM() << 6);
            }

            opl3out(0x40 + m, regm);
            modulatorVol[chan] = 63 - modulator;
        }

        if (carrier != BYTE_NULL) {
            int regc;

            fmparTable[chan].volC(carrier);

            if (volumeScaling) {
                carrier = scaleVolume(instr.fm.volC(), carrier);
            }

            carrier = scaleVolume(carrier, 63 - globalVolume);
            regc = scaleVolume(carrier, 63 - overallVolume) + (fmparTable[chan].kslC() << 6);

            opl3out(0x40 + c, regc);
            carrierVol[chan] = 63 - carrier;
        }
    }

    private void setVolume(int modulator, int carrier, int chan) {
        InstrData instr = getInstrDataByCh(chan);
        if (instr == null) instr = zeroins;

        // ** OPL3 emulation workaround ** (see setInsVolume)
        FmregTable fmreg = getFmregTable(voiceTable[chan]);
        int fmregLength = fmreg != null ? fmreg.length : 0;

        if (isChanAdsrDataEmpty(chan) && fmregLength == 0) {
            modulator = 63;
            carrier = 63;
        }

        int m = regoffsM(chan);
        int c = regoffsC(chan);

        if (modulator != BYTE_NULL) {
            int regm;
            fmparTable[chan].volM(modulator);

            modulator = scaleVolume(instr.fm.volM(), modulator);
            modulator = scaleVolume(modulator, 63 - globalVolume);

            regm = scaleVolume(modulator, 63 - overallVolume) + (fmparTable[chan].kslM() << 6);

            opl3out(0x40 + m, regm);
            modulatorVol[chan] = 63 - modulator;
        }

        if (carrier != BYTE_NULL) {
            int regc;
            fmparTable[chan].volC(carrier);

            carrier = scaleVolume(instr.fm.volC(), carrier);
            carrier = scaleVolume(carrier, 63 - globalVolume);

            regc = scaleVolume(carrier, 63 - overallVolume) + (fmparTable[chan].kslC() << 6);

            opl3out(0x40 + c, regc);
            carrierVol[chan] = 63 - carrier;
        }
    }

    private void setInsVolume4op(int volume, int chan) {
        FourOpData d = get4opData(chan);

        if (!is4opVolValidChan(chan)) return;

        int volM1 = BYTE_NULL;
        int volC1;
        int volM2 = BYTE_NULL;
        int volC2 = BYTE_NULL;

        volC1 = volume == BYTE_NULL ? fmparTable[d.ch1].volC() : volume;

        switch (d.conn) {
        case 0: // FM/FM ins1=FM, ins2=FM
            break;
        case 1: // FM/AM ins1=FM, ins2=AM
            volM2 = volume == BYTE_NULL ? fmparTable[d.ch2].volM() : volume;
            break;
        case 2: // AM/FM ins1=AM, ins2=FM
            volC2 = volume == BYTE_NULL ? fmparTable[d.ch2].volC() : volume;
            break;
        case 3: // AM/AM ins1=AM, ins2=AM
            volM1 = volume == BYTE_NULL ? fmparTable[d.ch1].volM() : volume;
            volM2 = volume == BYTE_NULL ? fmparTable[d.ch2].volM() : volume;
            break;
        }

        setVolume(volM1, volC1, d.ch1);
        setVolume(volM2, volC2, d.ch2);
    }

    private void resetInsVolume(int chan) {
        InstrData instr = getInstrDataByCh(chan);
        if (instr == null) return;

        int volMod = instr.fm.volM();
        int volCar = instr.fm.volC();
        int conn = instr.fm.connect();

        if (volumeScaling) {
            volMod = conn == 0 ? volMod : 0;
            volCar = 0;
        }

        setInsVolume(volMod, volCar, chan);
    }

    private void setGlobalVolume() {
        for (int chan = 0; chan < nmTracks; chan++) {
            if (is4opVolValidChan(chan)) {
                setInsVolume4op(BYTE_NULL, chan);
            } else if (carrierVol[chan] != 0 || modulatorVol[chan] != 0) {
                InstrData instr = getInstrDataByCh(chan);
                if (instr == null) instr = zeroins;

                setInsVolume(instr.fm.connect() != 0 ? fmparTable[chan].volM() : BYTE_NULL, fmparTable[chan].volC(), chan);
            }
        }
    }

    private void setOverallVolume(int level) {
        overallVolume = clampMax(level, 63);
        setGlobalVolume();
    }

    private void initMacroTable(int chan, int note, int ins, int freq) {
        InstrExt instrument = getInstr(ins);

        int arpTable = instrument != null ? instrument.arpeggio : 0;
        macroTable[chan].fmregPos = 0;
        macroTable[chan].fmregDuration = 0;
        macroTable[chan].fmregIns = ins;
        macroTable[chan].arpgCount = 1;
        macroTable[chan].arpgPos = 0;
        macroTable[chan].arpgTable = arpTable;
        macroTable[chan].arpgNote = note;

        int vibTable = instrument != null ? instrument.vibrato : 0;
        VibratoTable vib = getVibratoTable(vibTable);
        int vibDelay = vib != null ? vib.delay : 0;

        macroTable[chan].vibCount = 1;
        macroTable[chan].vibPaused = false;
        macroTable[chan].vibPos = 0;
        macroTable[chan].vibTable = vibTable;
        macroTable[chan].vibFreq = freq & 0xffff;
        macroTable[chan].vibDelay = vibDelay;

        zeroFqTable[chan] = 0;
    }

    private void setInsData(int ins, int chan) {
        if (ins == 0) return;

        InstrData i = getInstrData(ins);
        if (i == null) i = zeroins;

        if (i.isEmpty()) {
            releaseSustainingSound(chan);
        }

        if ((ins != eventTable[chan].instrDef) || resetChan[chan]) {
            panningTable[chan] = !panLock[chan]
                    ? i.panning
                    : lockFlags[chan] & 3;
            if (panningTable[chan] >= _panning.length) {
                panningTable[chan] = 0;
            }

            int m = regoffsM(chan);
            int c = regoffsC(chan);
            int n = regoffsN(chan);

            opl3out(0x20 + m, i.fm.data[0]);
            opl3out(0x20 + c, i.fm.data[1]);
            opl3out(0x40 + m, (i.fm.data[2] & 0xc0) + 63);
            opl3out(0x40 + c, (i.fm.data[3] & 0xc0) + 63);
            opl3out(0x60 + m, i.fm.data[4]);
            opl3out(0x60 + c, i.fm.data[5]);
            opl3out(0x80 + m, i.fm.data[6]);
            opl3out(0x80 + c, i.fm.data[7]);
            opl3out(0xe0 + m, i.fm.data[8]);
            opl3out(0xe0 + c, i.fm.data[9]);
            opl3out(0xc0 + n, i.fm.data[10] | _panning[panningTable[chan]]);

            for (int r = 0; r < 11; r++) {
                fmparTable[chan].data[r] = i.fm.data[r];
            }

            // stop instr macro if resetting voice
            if (!resetChan[chan]) {
                keyoffLoop[chan] = false;
            }

            if (resetChan[chan]) {
                voiceTable[chan] = ins;
                resetInsVolume(chan);
                resetChan[chan] = false;
            }

            int note = eventTable[chan].note & 0x7f;
            note = noteInRange(note) ? note : 0;

            initMacroTable(chan, note, ins, freqTable[chan]);
        }

        voiceTable[chan] = ins;
        int oldIns = eventTable[chan].instrDef;
        eventTable[chan].instrDef = ins;

        if (!volumeLock[chan] || (ins != oldIns)) {
            resetInsVolume(chan);
        }
    }

    private void updateModulatorAdsrw(int chan) {
        FmInstData fmpar = fmparTable[chan];
        int m = regoffsM(chan);

        opl3out(0x60 + m, fmpar.data[4]);
        opl3out(0x80 + m, fmpar.data[6]);
        opl3out(0xe0 + m, fmpar.data[8]);
    }

    private void updateCarrierAdsrw(int chan) {
        FmInstData fmpar = fmparTable[chan];
        int c = regoffsC(chan);

        opl3out(0x60 + c, fmpar.data[5]);
        opl3out(0x80 + c, fmpar.data[7]);
        opl3out(0xe0 + c, fmpar.data[9]);
    }

    private void updateFmpar(int chan) {
        FmInstData fmpar = fmparTable[chan];

        opl3out(0x20 + regoffsM(chan), fmpar.data[0]);
        opl3out(0x20 + regoffsC(chan), fmpar.data[1]);
        opl3out(0xc0 + regoffsN(chan), fmpar.data[10] | _panning[panningTable[chan]]);

        setInsVolume(fmpar.volM(), fmpar.volC(), chan);
    }

    private static final int[] _4op_mask = {
        1, 1, 1 << 1, 1 << 1, 1 << 2, 1 << 2, 0, 0, 0,
        1 << 3, 1 << 3, 1 << 4, 1 << 4, 1 << 5, 1 << 5, 0, 0, 0, 0, 0
    };

    private boolean is4opChan(int chan) { // 0..19
        return chan > 14 ? false : (flag4op & _4op_mask[chan]) != 0;
    }

    private static final boolean[] _4op_hi = {
        true, false, true, false, true, false, false, false, false,                 // 0, 2, 4
        true, false, true, false, true, false, false, false, false, false, false   // 9, 11, 13
    };

    private static final boolean[] _4op_lo = {
        false, true, false, true, false, true, false, false, false,                // 1, 3, 5
        false, true, false, true, false, true, false, false, false, false, false   // 10, 12, 14
    };

    private boolean is4opChanHi(int chan) {
        return _4op_hi[chan];
    }

    private boolean is4opChanLo(int chan) {
        return _4op_lo[chan];
    }

    private void outputNote(int note, int ins, int chan, boolean restartMacro, boolean restartAdsr) {
        int freq;

        if ((note == 0) && (ftuneTable[chan] == 0)) return;

        if ((note & 0x80) != 0 || !noteInRange(note)) {
            freq = freqTable[chan];
        } else {
            freq = (nFreq(note - 1) + getInstrFineTune(ins)) & 0xffff;

            if (restartAdsr) {
                keyOn(chan);
            }

            freqTable[chan] |= 0x2000;
        }

        if (ftuneTable[chan] == -127) {
            ftuneTable[chan] = 0;
        }

        freq = (freq + ftuneTable[chan]) & 0xffff;
        changeFrequency(chan, freq);

        if (note != 0) {
            eventTable[chan].note = note;

            if (is4opChan(chan) && is4opChanLo(chan)) {
                eventTable[chan - 1].note = note;
            }

            if (restartMacro) {
                // check if no ZFF - force no restart
                boolean forceNoRestart =
                        ((eventTable[chan].effDef[0] == ef_Extended) &&
                         (eventTable[chan].effVal[0] == ef_ex_ExtendedCmd2 * 16 + ef_ex_cmd2_NoRestart)) ||
                        ((eventTable[chan].effDef[1] == ef_Extended) &&
                         (eventTable[chan].effVal[1] == ef_ex_ExtendedCmd2 * 16 + ef_ex_cmd2_NoRestart));
                if (!forceNoRestart) {
                    initMacroTable(chan, note, ins, freq);
                } else {
                    macroTable[chan].arpgNote = note;
                }
            }
        }
    }

    private boolean noLoop(int currentChan, int currentLine) {
        for (int chan = 0; chan < currentChan; chan++) {
            if (loopTable[chan][currentLine] != 0 && loopTable[chan][currentLine] != BYTE_NULL) {
                return false;
            }
        }

        return true;
    }

    private static int getEffectGroup(int def) {
        switch (def) {
        case ef_ArpggVSlide:
        case ef_ArpggVSlideFine:    return EFGR_ARPVOLSLIDE;
        case ef_FSlideUpVSlide:
        case ef_FSlUpVSlF:
        case ef_FSlideDownVSlide:
        case ef_FSlDownVSlF:
        case ef_FSlUpFineVSlide:
        case ef_FSlUpFineVSlF:
        case ef_FSlDownFineVSlide:
        case ef_FSlDownFineVSlF:    return EFGR_FSLIDEVOLSLIDE;
        case ef_TonePortamento:     return EFGR_TONEPORTAMENTO;
        case ef_Vibrato:
        case ef_ExtraFineVibrato:   return EFGR_VIBRATO;
        case ef_Tremolo:
        case ef_ExtraFineTremolo:   return EFGR_TREMOLO;
        case ef_VibratoVolSlide:
        case ef_VibratoVSlideFine:  return EFGR_VIBRATOVOLSLIDE;
        case ef_TPortamVolSlide:
        case ef_TPortamVSlideFine:  return EFGR_PORTAVOLSLIDE;
        case ef_RetrigNote:
        case ef_MultiRetrigNote:    return EFGR_RETRIGNOTE;
        default: return -1;
        }
    }

    /** in case of x00 set value of the previous compatible effect command */
    private void updateEffectTable(int slot, int chan, int effGroup, int def, int val) {
        int lval = lastEffectVal[slot][chan];

        effectTableDef[slot][chan] = def;

        if (val != 0) {
            effectTableVal[slot][chan] = val;
        } else if (getEffectGroup(lastEffectDef[slot][chan]) == effGroup && lval != 0) {
            effectTableVal[slot][chan] = lval;
        } else {
            // x00 without any previous compatible command, should never happen
            effectTableDef[slot][chan] = 0;
            effectTableVal[slot][chan] = 0;
        }
    }

    private void processEffects(A2Event event, int slot, int chan) {
        InstrData instr = getInstrDataByCh(chan);
        if (instr == null) instr = zeroins;
        int def = event.effDef[slot];
        int val = event.effVal[slot];

        effectTableDef[slot][chan] = def;
        effectTableVal[slot][chan] = val;

        if (def != ef_Vibrato &&
                def != ef_ExtraFineVibrato &&
                def != ef_VibratoVolSlide &&
                def != ef_VibratoVSlideFine) {
            vibrTablePos[slot][chan] = 0;
            vibrTableSpeed[slot][chan] = 0;
            vibrTableDepth[slot][chan] = 0;
            vibrTableFine[slot][chan] = false;
        }

        if (def != ef_RetrigNote && def != ef_MultiRetrigNote) {
            retrigTable[slot][chan] = 0;
        }

        if (def != ef_Tremolo && def != ef_ExtraFineTremolo) {
            tremTablePos[slot][chan] = 0;
            tremTableSpeed[slot][chan] = 0;
            tremTableDepth[slot][chan] = 0;
            tremTableFine[slot][chan] = false;
        }

        if (!(((def == ef_Arpeggio) && (val != 0)) || (def == ef_ExtraFineArpeggio)) &&
                (arpggTableNote[slot][chan] != 0) && (arpggTableState[slot][chan] != 1)) {
            arpggTableState[slot][chan] = 1;
            changeFrequency(chan, (nFreq(arpggTableNote[slot][chan] - 1) +
                    getInstrFineTune(eventTable[chan].instrDef)) & 0xffff);
        }

        if ((def == ef_GlobalFSlideUp) || (def == ef_GlobalFSlideDown)) {
            if ((event.effDef[slot ^ 1] == ef_Extended) &&
                    (event.effVal[slot ^ 1] == ef_ex_ExtendedCmd * 16 + ef_ex_cmd_ForceBpmSld)) {

                if (def == ef_GlobalFSlideUp) {
                    updatePlaybackSpeed(val);
                } else {
                    updatePlaybackSpeed(-val);
                }
            } else {
                int eff;

                switch (def) {
                case ef_GlobalFSlideUp:
                    eff = ef_FSlideUp;

                    // >xx + ZFE
                    if ((event.effDef[slot ^ 1] == ef_Extended) &&
                            (event.effVal[slot ^ 1] == ef_ex_ExtendedCmd2 * 16 + ef_ex_cmd2_FTrm_XFGFS)) {
                        eff = ef_GlobalFreqSlideUpXF;
                    }

                    // >xx + ZFD
                    if ((event.effDef[slot ^ 1] == ef_Extended) &&
                            (event.effVal[slot ^ 1] == ef_ex_ExtendedCmd2 * 16 + ef_ex_cmd2_FVib_FGFS)) {
                        eff = ef_FSlideUpFine;
                    }

                    effectTableDef[slot][chan] = eff;
                    effectTableVal[slot][chan] = val;
                    break;
                case ef_GlobalFSlideDown:
                    eff = ef_FSlideDown;

                    // <xx + ZFE
                    if ((event.effDef[slot ^ 1] == ef_Extended) &&
                            (event.effVal[slot ^ 1] == ef_ex_ExtendedCmd2 * 16 + ef_ex_cmd2_FTrm_XFGFS)) {
                        eff = ef_GlobalFreqSlideDnXF;
                    }

                    // <xx + ZFD
                    if ((event.effDef[slot ^ 1] == ef_Extended) &&
                            (event.effVal[slot ^ 1] == ef_ex_ExtendedCmd2 * 16 + ef_ex_cmd2_FVib_FGFS)) {
                        eff = ef_FSlideDownFine;
                    }

                    effectTableDef[slot][chan] = eff;
                    effectTableVal[slot][chan] = val;
                    break;
                }

                // shouldn't it be int c = 0 ?? (kept as in adplug)
                for (int c = chan; c < nmTracks; c++) {
                    fslideTable[slot][c] = val;
                    glfsldTableDef[slot][c] = effectTableDef[slot][chan];
                    glfsldTableVal[slot][c] = effectTableVal[slot][chan];
                }
            }
        }

        if (tremorTablePos[slot][chan] != 0 && def != ef_Tremor) {
            tremorTablePos[slot][chan] = 0;
            setInsVolume(tremorTableVolM[slot][chan], tremorTableVolC[slot][chan], chan);
        }

        switch (def) {
        case ef_Arpeggio:
            if (val == 0) break;
            // fall through
        case ef_ExtraFineArpeggio:
        case ef_ArpggVSlide:
        case ef_ArpggVSlideFine:
            switch (def) {
            case ef_Arpeggio:
                effectTableDef[slot][chan] = ef_Arpeggio;
                effectTableVal[slot][chan] = val;
                break;
            case ef_ExtraFineArpeggio:
                effectTableDef[slot][chan] = ef_ExtraFineArpeggio;
                effectTableVal[slot][chan] = val;
                break;
            case ef_ArpggVSlide:
            case ef_ArpggVSlideFine:
                updateEffectTable(slot, chan, EFGR_ARPVOLSLIDE, def, val);
                break;
            }

            if (noteInRange(event.note)) {
                arpggTableState[slot][chan] = 0;
                arpggTableNote[slot][chan] = event.note & 0x7f;
                if ((def == ef_Arpeggio) || (def == ef_ExtraFineArpeggio)) {
                    arpggTableAdd1[slot][chan] = val >> 4;
                    arpggTableAdd2[slot][chan] = val & 0x0f;
                }
            } else {
                if (event.note == 0 && noteInRange(eventTable[chan].note)) {
                    arpggTableNote[slot][chan] = eventTable[chan].note & 0x7f;
                    if ((def == ef_Arpeggio) || (def == ef_ExtraFineArpeggio)) {
                        arpggTableAdd1[slot][chan] = val / 16;
                        arpggTableAdd2[slot][chan] = val % 16;
                    }
                } else {
                    effectTableDef[slot][chan] = 0;
                    effectTableVal[slot][chan] = 0;
                }
            }
            break;

        case ef_FSlideUp:
        case ef_FSlideDown:
        case ef_FSlideUpFine:
        case ef_FSlideDownFine:
            effectTableDef[slot][chan] = def;
            effectTableVal[slot][chan] = val;
            fslideTable[slot][chan] = val;
            break;

        case ef_FSlideUpVSlide:
        case ef_FSlUpVSlF:
        case ef_FSlideDownVSlide:
        case ef_FSlDownVSlF:
        case ef_FSlUpFineVSlide:
        case ef_FSlUpFineVSlF:
        case ef_FSlDownFineVSlide:
        case ef_FSlDownFineVSlF:
            updateEffectTable(slot, chan, EFGR_FSLIDEVOLSLIDE, def, val);
            break;

        case ef_TonePortamento:
            updateEffectTable(slot, chan, EFGR_TONEPORTAMENTO, def, val);

            if (noteInRange(event.note)) {
                portaTableSpeed[slot][chan] = val;
                portaTableFreq[slot][chan] = (nFreq(event.note - 1) +
                        getInstrFineTune(eventTable[chan].instrDef)) & 0xffff;
            } else {
                portaTableSpeed[slot][chan] = effectTableVal[slot][chan];
            }
            break;

        case ef_TPortamVolSlide:
        case ef_TPortamVSlideFine:
            updateEffectTable(slot, chan, EFGR_PORTAVOLSLIDE, def, val);
            break;

        case ef_Vibrato:
        case ef_ExtraFineVibrato:
            updateEffectTable(slot, chan, EFGR_VIBRATO, def, val);

            if ((event.effDef[slot ^ 1] == ef_Extended) &&
                    (event.effVal[slot ^ 1] == ef_ex_ExtendedCmd2 * 16 + ef_ex_cmd2_FVib_FGFS)) {
                vibrTableFine[slot][chan] = true;
            }

            vibrTableSpeed[slot][chan] = val / 16;
            vibrTableDepth[slot][chan] = val % 16;
            break;

        case ef_Tremolo:
        case ef_ExtraFineTremolo:
            updateEffectTable(slot, chan, EFGR_TREMOLO, def, val);

            if ((event.effDef[slot ^ 1] == ef_Extended) &&
                    (event.effVal[slot ^ 1] == ef_ex_ExtendedCmd2 * 16 + ef_ex_cmd2_FTrm_XFGFS)) {
                tremTableFine[slot][chan] = true;
            }

            tremTableSpeed[slot][chan] = val / 16;
            tremTableDepth[slot][chan] = val % 16;
            break;

        case ef_VibratoVolSlide:
        case ef_VibratoVSlideFine:
            updateEffectTable(slot, chan, EFGR_VIBRATOVOLSLIDE, def, val);

            if ((event.effDef[slot ^ 1] == ef_Extended) &&
                    (event.effVal[slot ^ 1] == ef_ex_ExtendedCmd2 * 16 + ef_ex_cmd2_FVib_FGFS)) {
                vibrTableFine[slot][chan] = true;
            }
            break;

        case ef_SetCarrierVol:
            setInsVolume(BYTE_NULL, 63 - val, chan);
            break;

        case ef_SetModulatorVol:
            setInsVolume(63 - val, BYTE_NULL, chan);
            break;

        case ef_SetInsVolume:
            if (is4opVolValidChan(chan)) {
                setInsVolume4op(63 - val, chan);
            } else if (percussionMode && ((chan >= 16) && (chan <= 19))) { // in [17..20]
                setInsVolume(63 - val, BYTE_NULL, chan);
            } else if (instr.fm.connect() == 0) {
                setInsVolume(BYTE_NULL, 63 - val, chan);
            } else {
                setInsVolume(63 - val, 63 - val, chan);
            }
            break;

        case ef_ForceInsVolume:
            if (percussionMode && ((chan >= 16) && (chan <= 19))) { // in [17..20]
                setInsVolume(63 - val, BYTE_NULL, chan);
            } else if (instr.fm.connect() == 0) {
                setInsVolume(scaleVolume(instr.fm.volM(), 63 - val), 63 - val, chan);
            } else {
                setInsVolume(63 - val, 63 - val, chan);
            }
            break;

        case ef_PositionJump:
            if (noLoop(chan, currentLine)) {
                patternBreak = true;
                nextLine = pattern_break_flag + chan;
            }
            break;

        case ef_PatternBreak:
            if (noLoop(chan, currentLine)) {
                patternBreak = true;
                nextLine = clampMax(val, pattLen - 1);
            }
            break;

        case ef_SetSpeed:
            speed = val;
            break;

        case ef_SetTempo:
            updateTimer(val);
            break;

        case ef_SetWaveform:
            if (val / 16 <= 7) { // in [0..7]
                fmparTable[chan].wformC(val / 16);
                updateCarrierAdsrw(chan);
            }

            if (val % 16 <= 7) { // in [0..7]
                fmparTable[chan].wformM(val % 16);
                updateModulatorAdsrw(chan);
            }
            break;

        case ef_VolSlide:
        case ef_VolSlideFine:
            effectTableDef[slot][chan] = def;
            effectTableVal[slot][chan] = val;
            break;

        case ef_RetrigNote:
        case ef_MultiRetrigNote:
            if (val != 0) {
                if (getEffectGroup(lastEffectDef[slot][chan]) != EFGR_RETRIGNOTE) {
                    retrigTable[slot][chan] = 1;
                }

                effectTableDef[slot][chan] = def;
                effectTableVal[slot][chan] = val;
            }
            break;

        case ef_SetGlobalVolume:
            globalVolume = val;
            setGlobalVolume();
            break;

        case ef_Tremor:
            if (val != 0) {
                if (lastEffectDef[slot][chan] != ef_Tremor) {
                    tremorTablePos[slot][chan] = 0;
                    tremorTableVolM[slot][chan] = fmparTable[chan].volM();
                    tremorTableVolC[slot][chan] = fmparTable[chan].volC();
                }

                effectTableDef[slot][chan] = def;
                effectTableVal[slot][chan] = val;
            }
            break;

        case ef_Extended:
            switch (val / 16) {
            case ef_ex_SetTremDepth:
                switch (val % 16) {
                case 0:
                    opl3out(0xbd, miscRegister & 0x7f);
                    currentTremoloDepth = 0;
                    break;

                case 1:
                    opl3out(0xbd, miscRegister | 0x80);
                    currentTremoloDepth = 1;
                    break;
                }
                break;

            case ef_ex_SetVibDepth:
                switch (val % 16) {
                case 0:
                    opl3out(0xbd, miscRegister & 0xbf);
                    currentVibratoDepth = 0;
                    break;

                case 1:
                    opl3out(0xbd, miscRegister | 0x40);
                    currentVibratoDepth = 1;
                    break;
                }
                break;

            case ef_ex_SetAttckRateM:
                fmparTable[chan].attckM(val % 16);
                updateModulatorAdsrw(chan);
                break;

            case ef_ex_SetDecayRateM:
                fmparTable[chan].decM(val % 16);
                updateModulatorAdsrw(chan);
                break;

            case ef_ex_SetSustnLevelM:
                fmparTable[chan].sustnM(val % 16);
                updateModulatorAdsrw(chan);
                break;

            case ef_ex_SetRelRateM:
                fmparTable[chan].relM(val % 16);
                updateModulatorAdsrw(chan);
                break;

            case ef_ex_SetAttckRateC:
                fmparTable[chan].attckC(val % 16);
                updateCarrierAdsrw(chan);
                break;

            case ef_ex_SetDecayRateC:
                fmparTable[chan].decC(val % 16);
                updateCarrierAdsrw(chan);
                break;

            case ef_ex_SetSustnLevelC:
                fmparTable[chan].sustnC(val % 16);
                updateCarrierAdsrw(chan);
                break;

            case ef_ex_SetRelRateC:
                fmparTable[chan].relC(val % 16);
                updateCarrierAdsrw(chan);
                break;

            case ef_ex_SetFeedback:
                fmparTable[chan].feedb(val % 16);
                updateFmpar(chan);
                break;

            case ef_ex_SetPanningPos:
                panningTable[chan] = val % 16;
                updateFmpar(chan);
                break;

            case ef_ex_PatternLoop:
            case ef_ex_PatternLoopRec:
                if (val % 16 == 0) {
                    loopbckTable[chan] = currentLine;
                } else {
                    if (loopbckTable[chan] != BYTE_NULL) {
                        if (loopTable[chan][currentLine] == BYTE_NULL) {
                            loopTable[chan][currentLine] = val % 16;
                        }

                        if (loopTable[chan][currentLine] != 0) {
                            patternBreak = true;
                            nextLine = pattern_loop_flag + chan;
                        } else {
                            if (val / 16 == ef_ex_PatternLoopRec) {
                                loopTable[chan][currentLine] = BYTE_NULL;
                            }
                        }
                    }
                }
                break;
            case ef_ex_ExtendedCmd:
                switch (val & 0x0f) {
                case ef_ex_cmd_MKOffLoopDi: keyoffLoop[chan] = false;  break;
                case ef_ex_cmd_MKOffLoopEn: keyoffLoop[chan] = true;   break;
                case ef_ex_cmd_TPortaFKdis: portaFKTable[chan] = false; break;
                case ef_ex_cmd_TPortaFKenb: portaFKTable[chan] = true;  break;
                case ef_ex_cmd_RestartEnv:
                    keyOn(chan);
                    changeFreq(chan, freqTable[chan]);
                    break;
                case ef_ex_cmd_4opVlockOff:
                    if (is4opChan(chan)) {
                        vol4opLock[chan] = false;
                        int i = is4opChanHi(chan) ? 1 : -1;

                        vol4opLock[chan + i] = false;
                    }
                    break;
                case ef_ex_cmd_4opVlockOn:
                    if (is4opChan(chan)) {
                        vol4opLock[chan] = true;
                        int i = is4opChanHi(chan) ? 1 : -1;

                        vol4opLock[chan + i] = true;
                    }
                    break;
                }
                break;
            case ef_ex_ExtendedCmd2:
                switch (val % 16) {
                case ef_ex_cmd2_RSS:        releaseSustainingSound(chan); break;
                case ef_ex_cmd2_ResetVol:   resetInsVolume(chan); break;
                case ef_ex_cmd2_LockVol:    volumeLock[chan] = true; break;
                case ef_ex_cmd2_UnlockVol:  volumeLock[chan] = false; break;
                case ef_ex_cmd2_LockVP:     peakLock[chan] = true; break;
                case ef_ex_cmd2_UnlockVP:   peakLock[chan] = false; break;
                case ef_ex_cmd2_VSlide_def: volslideType[chan] = 0; break;
                case ef_ex_cmd2_LockPan:    panLock[chan] = true; break;
                case ef_ex_cmd2_UnlockPan:  panLock[chan] = false; break;
                case ef_ex_cmd2_VibrOff:    changeFrequency(chan, freqTable[chan]); break;
                case ef_ex_cmd2_TremOff:
                    if (is4opChan(chan)) {
                        setInsVolume4op(BYTE_NULL, chan);
                    } else {
                        setInsVolume(fmparTable[chan].volM(), fmparTable[chan].volC(), chan);
                    }
                    break;
                case ef_ex_cmd2_VSlide_car:
                    if ((event.effDef[slot ^ 1] == ef_Extended) &&
                            (event.effVal[slot ^ 1] == ef_ex_ExtendedCmd2 * 16 + ef_ex_cmd2_VSlide_mod)) {
                        volslideType[chan] = 3;
                    } else {
                        volslideType[chan] = 1;
                    }
                    break;

                case ef_ex_cmd2_VSlide_mod:
                    if ((event.effDef[slot ^ 1] == ef_Extended) &&
                            (event.effVal[slot ^ 1] == ef_ex_ExtendedCmd2 * 16 + ef_ex_cmd2_VSlide_car)) {
                        volslideType[chan] = 3;
                    } else {
                        volslideType[chan] = 2;
                    }
                    break;
                }
                break;
            }
            break;

        case ef_Extended2:
            switch (val / 16) {
            case ef_ex2_PatDelayFrame:
                patternDelay = true;
                tickD = val % 16;
                break;

            case ef_ex2_PatDelayRow:
                patternDelay = true;
                tickD = speed * (val % 16);
                break;

            case ef_ex2_NoteDelay:
                effectTableDef[slot][chan] = ef_Extended2;
                effectTableVal[slot][chan] = val;
                notedelTable[chan] = val % 16;
                break;

            case ef_ex2_NoteCut:
                effectTableDef[slot][chan] = ef_Extended2;
                effectTableVal[slot][chan] = val;
                notecutTable[chan] = val % 16;
                break;

            case ef_ex2_FineTuneUp:
                ftuneTable[chan] += val % 16;
                break;

            case ef_ex2_FineTuneDown:
                ftuneTable[chan] -= val % 16;
                break;

            case ef_ex2_GlVolSlideUp:
            case ef_ex2_GlVolSlideDn:
            case ef_ex2_GlVolSlideUpF:
            case ef_ex2_GlVolSlideDnF:
            case ef_ex2_GlVolSldUpXF:
            case ef_ex2_GlVolSldDnXF:
            case ef_ex2_VolSlideUpXF:
            case ef_ex2_VolSlideDnXF:
            case ef_ex2_FreqSlideUpXF:
            case ef_ex2_FreqSlideDnXF:
                effectTableDef[slot][chan] = ef_Extended2;
                effectTableVal[slot][chan] = val;
                break;
            }
            break;

        case ef_Extended3:
            switch (val / 16) {
            case ef_ex3_SetConnection:
                fmparTable[chan].connect(val % 16);
                updateFmpar(chan);
                break;

            case ef_ex3_SetMultipM:
                fmparTable[chan].multipM(val % 16);
                updateFmpar(chan);
                break;

            case ef_ex3_SetKslM:
                fmparTable[chan].kslM(val % 16);
                updateFmpar(chan);
                break;

            case ef_ex3_SetTremoloM:
                fmparTable[chan].tremM(val % 16);
                updateFmpar(chan);
                break;

            case ef_ex3_SetVibratoM:
                fmparTable[chan].vibrM(val % 16);
                updateFmpar(chan);
                break;

            case ef_ex3_SetKsrM:
                fmparTable[chan].ksrM(val % 16);
                updateFmpar(chan);
                break;

            case ef_ex3_SetSustainM:
                fmparTable[chan].sustM(val % 16);
                updateFmpar(chan);
                break;

            case ef_ex3_SetMultipC:
                fmparTable[chan].multipC(val % 16);
                updateFmpar(chan);
                break;

            case ef_ex3_SetKslC:
                fmparTable[chan].kslC(val % 16);
                updateFmpar(chan);
                break;

            case ef_ex3_SetTremoloC:
                fmparTable[chan].tremC(val % 16);
                updateFmpar(chan);
                break;

            case ef_ex3_SetVibratoC:
                fmparTable[chan].vibrC(val % 16);
                updateFmpar(chan);
                break;

            case ef_ex3_SetKsrC:
                fmparTable[chan].ksrC(val % 16);
                updateFmpar(chan);
                break;

            case ef_ex3_SetSustainC:
                fmparTable[chan].sustC(val % 16);
                updateFmpar(chan);
                break;
            }
            break;
        }
    }

    private static boolean noSwapAndRestart(A2Event event) {
        // [!xx/@xx] swap arp/swap vib + [zff] no force restart
        return !(((event.effDef[1] == ef_SwapArpeggio) ||
                (event.effDef[1] == ef_SwapVibrato)) &&
                (event.effDef[0] == ef_Extended) &&
                (event.effVal[0] == ef_ex_ExtendedCmd2 * 16 + ef_ex_cmd2_NoRestart)) &&
                !(((event.effDef[0] == ef_SwapArpeggio) ||
                (event.effDef[0] == ef_SwapVibrato)) &&
                (event.effDef[1] == ef_Extended) &&
                (event.effVal[1] == ef_ex_ExtendedCmd2 * 16 + ef_ex_cmd2_NoRestart));
    }

    private static boolean isEffPorta(A2Event event) {
        int eff0 = event.effDef[0];
        boolean isP0 = (eff0 == ef_TonePortamento) ||
                (eff0 == ef_TPortamVolSlide) ||
                (eff0 == ef_TPortamVSlideFine);
        int eff1 = event.effDef[1];
        boolean isP1 = (eff1 == ef_TonePortamento) ||
                (eff1 == ef_TPortamVolSlide) ||
                (eff1 == ef_TPortamVSlideFine);
        return isP0 || isP1;
    }

    private static boolean isEffNotedelay(A2Event event) {
        return (event.effDef[0] == ef_Extended2 && (event.effVal[0] / 16 == ef_ex2_NoteDelay)) ||
                (event.effDef[1] == ef_Extended2 && (event.effVal[1] / 16 == ef_ex2_NoteDelay));
    }

    private void newProcessNote(A2Event event, int chan) {
        boolean tportaFlag = isEffPorta(event);
        boolean notedelayFlag = isEffNotedelay(event);

        if (event.note == 0) return;

        // this might delay even note-off
        if (notedelayFlag) {
            eventTable[chan].note = event.note;
            return;
        }

        if ((event.note & keyoff_flag) != 0) {
            keyOff(chan);
            return;
        }

        if (!tportaFlag) {
            outputNote(event.note, voiceTable[chan], chan, true, noSwapAndRestart(event));
            return;
        }

        // if previous note was off'ed or restart_adsr enabled for channel
        // and we are doing portamento to a new note
        if ((eventTable[chan].note & keyoff_flag) != 0 || portaFKTable[chan]) {
            outputNote(eventTable[chan].note & ~keyoff_flag, voiceTable[chan], chan, false, true);
        } else {
            eventTable[chan].note = event.note;
        }
    }

    private void playLine() {
        A2Event event = new A2Event();

        for (int chan = 0; chan < nmTracks; chan++) {
            // save effect_table into last_effect
            for (int slot = 0; slot < 2; slot++) {
                if ((effectTableDef[slot][chan] | effectTableVal[slot][chan]) != 0) {
                    lastEffectDef[slot][chan] = effectTableDef[slot][chan];
                    lastEffectVal[slot][chan] = effectTableVal[slot][chan];
                }
                if ((glfsldTableDef[slot][chan] | glfsldTableVal[slot][chan]) != 0) {
                    effectTableDef[slot][chan] = glfsldTableDef[slot][chan];
                    effectTableVal[slot][chan] = glfsldTableVal[slot][chan];
                } else {
                    effectTableDef[slot][chan] = 0;
                    effectTableVal[slot][chan] = 0;
                }
            }

            ftuneTable[chan] = 0;

            // do a full copy of the event, because we modify event.note
            event.copyFrom(getEventP(currentPattern, chan, currentLine));

            // fixup event.note
            if (event.note == 0xff) { // key off
                event.note = eventTable[chan].note | keyoff_flag;
            } else if (event.note >= fixed_note_flag + 1) {
                event.note -= fixed_note_flag;
            }

            for (int slot = 0; slot < 2; slot++) {
                eventTable[chan].effDef[slot] = event.effDef[slot];
                eventTable[chan].effVal[slot] = event.effVal[slot];
            }

            // alters eventTable[].instrDef
            setInsData(event.instrDef, chan);

            // set effect_table here
            processEffects(event, 0, chan);
            processEffects(event, 1, chan);

            // alters eventTable[].note
            newProcessNote(event, chan);

            checkSwapArpVibr(event, 0, chan);
            checkSwapArpVibr(event, 1, chan);

            updateFineEffects(0, chan);
            updateFineEffects(1, chan);
        }
    }

    private void generateCustomVibrato(int value) {
        final int[] vibtabSize = { 16, 16, 16, 16, 32, 32, 32, 32, 64, 64, 64, 64, 128, 128, 128, 128 };
        int idx, idx2;

        if (value == 0) {
            // 0: set default speed table
            vibtremTableSize = def_vibtrem_table_size;
            System.arraycopy(def_vibtrem_table, 0, vibtremTable, 0, 256);
        } else if (value <= 239) {
            // 1-239: set custom speed table (fixed size = 32)
            vibtremTableSize = def_vibtrem_table_size;
            double mulR = (double) value / 16.0;

            for (idx2 = 0; idx2 <= 7; idx2++) {
                vibtremTable[idx2 * 32] = 0;

                for (idx = 1; idx <= 16; idx++) {
                    vibtremTable[idx2 * 32 + idx] = (int) Math.round(idx * mulR) & 0xff;
                }

                for (idx = 17; idx <= 31; idx++) {
                    vibtremTable[idx2 * 32 + idx] = (int) Math.round((32 - idx) * mulR) & 0xff;
                }
            }
        } else {
            // 240-255: set custom speed table (speed factor = 1-4)
            vibtremSpeedFactor = (value - 240) % 4 + 1;
            vibtremTableSize = 2 * vibtabSize[value - 240];
            int mulB = 256 / vibtabSize[value - 240];

            for (idx2 = 0; idx2 <= 128 / vibtabSize[value - 240] - 1; idx2++) {
                vibtremTable[2 * vibtabSize[value - 240] * idx2] = 0;

                for (idx = 1; idx <= vibtabSize[value - 240]; idx++) {
                    vibtremTable[2 * vibtabSize[value - 240] * idx2 + idx] =
                            Math.max(idx * mulB - 1, 0) & 0xff;
                }

                for (idx = vibtabSize[value - 240] + 1; idx <= 2 * vibtabSize[value - 240] - 1; idx++) {
                    vibtremTable[2 * vibtabSize[value - 240] * idx2 + idx] =
                            Math.max((2 * vibtabSize[value - 240] - idx) * mulB - 1, 0) & 0xff;
                }
            }
        }
    }

    private void checkSwapArpVibr(A2Event event, int slot, int chan) {
        // check if second effect is ZFF - force no restart
        boolean isNorestart = (event.effDef[slot ^ 1] == ef_Extended) &&
                (event.effVal[slot ^ 1] == (ef_ex_ExtendedCmd2 * 16 + ef_ex_cmd2_NoRestart));

        switch (event.effDef[slot]) {
        case ef_SwapArpeggio:
            if (isNorestart) {
                ArpeggioTable arp = getArpeggioTable(event.effVal[slot]);
                int length = arp != null ? arp.length : 0;

                if (macroTable[chan].arpgPos > length) {
                    macroTable[chan].arpgPos = length;
                }
                macroTable[chan].arpgTable = event.effVal[slot];
            } else {
                macroTable[chan].arpgCount = 1;
                macroTable[chan].arpgPos = 0;
                macroTable[chan].arpgTable = event.effVal[slot];
                macroTable[chan].arpgNote = eventTable[chan].note;
            }
            break;

        case ef_SwapVibrato:
            if (isNorestart) {
                VibratoTable vib = getVibratoTable(event.effVal[slot]);
                int length = vib != null ? vib.length : 0;

                if (macroTable[chan].vibPos > length) {
                    macroTable[chan].vibPos = length;
                }
                macroTable[chan].vibTable = event.effVal[slot];
            } else {
                VibratoTable vib = getVibratoTable(macroTable[chan].vibTable);
                int vibDelay = vib != null ? vib.delay : 0;

                macroTable[chan].vibCount = 1;
                macroTable[chan].vibPos = 0;
                macroTable[chan].vibTable = event.effVal[slot];
                macroTable[chan].vibDelay = vibDelay;
            }
            break;
        case ef_SetCustomSpeedTab:
            generateCustomVibrato(event.effVal[slot]);
            break;
        }
    }

    private void portamentoUp(int chan, int slide, int limit) {
        if ((freqTable[chan] & 0x1fff) == 0) return;

        int freq = calcFreqShiftUp(freqTable[chan] & 0x1fff, slide);

        changeFrequency(chan, freq <= limit ? freq : limit);
    }

    private void portamentoDown(int chan, int slide, int limit) {
        if ((freqTable[chan] & 0x1fff) == 0) return;

        int freq = calcFreqShiftDown(freqTable[chan] & 0x1fff, slide);

        changeFrequency(chan, freq >= limit ? freq : limit);
    }

    private void macroVibratoPortaUp(int chan, int depth) {
        int freq = calcFreqShiftUp(macroTable[chan].vibFreq & 0x1fff, depth);
        int newfreq = freq <= nFreq(12 * 8 + 1) ? freq : nFreq(12 * 8 + 1);

        changeFreq(chan, newfreq);
    }

    private void macroVibratoPortaDown(int chan, int depth) {
        int freq = calcFreqShiftDown(macroTable[chan].vibFreq & 0x1fff, depth);
        int newfreq = freq >= nFreq(0) ? freq : nFreq(0);

        changeFreq(chan, newfreq);
    }

    private void tonePortamento(int slot, int chan) {
        int freq = freqTable[chan] & 0x1fff;

        if (freq > portaTableFreq[slot][chan]) {
            portamentoDown(chan, portaTableSpeed[slot][chan], portaTableFreq[slot][chan]);
        } else if (freq < portaTableFreq[slot][chan]) {
            portamentoUp(chan, portaTableSpeed[slot][chan], portaTableFreq[slot][chan]);
        }
    }

    private void slideCarrierVolumeUp(int chan, int slide, int limit) {
        int volC = fmparTable[chan].volC();
        int newvolC = (volC - slide >= limit) ? volC - slide : limit;

        setInsVolume(BYTE_NULL, newvolC, chan);
    }

    private void slideModulatorVolumeUp(int chan, int slide, int limit) {
        int volM = fmparTable[chan].volM();
        int newvolM = (volM - slide >= limit) ? volM - slide : limit;

        setInsVolume(newvolM, BYTE_NULL, chan);
    }

    private void slideVolumeUp(int chan, int slide) {
        int limit1 = 0, limit2 = 0;
        FourOpData d = get4opData(chan);

        if (!is4opVolValidChan(chan)) {
            InstrData ins = getInstrData(eventTable[chan].instrDef);
            if (ins == null) ins = zeroins;

            limit1 = peakLock[chan] ? ins.fm.volC() : 0;
            limit2 = peakLock[chan] ? ins.fm.volM() : 0;
        }

        switch (volslideType[chan]) {
        case 0:
            if (!is4opVolValidChan(chan)) {
                InstrData i = getInstrDataByCh(chan);
                if (i == null) i = zeroins;

                slideCarrierVolumeUp(chan, slide, limit1);

                if (i.fm.connect() != 0 || (percussionMode && (chan >= 16))) { // in [17..20]
                    slideModulatorVolumeUp(chan, slide, limit2);
                }
            } else {
                InstrData ins1 = getInstrData(d.ins1);
                InstrData ins2 = getInstrData(d.ins2);
                if (ins1 == null) ins1 = zeroins;
                if (ins2 == null) ins2 = zeroins;

                int limit1VolC = peakLock[d.ch1] ? ins1.fm.volC() : 0;
                int limit1VolM = peakLock[d.ch1] ? ins1.fm.volM() : 0;
                int limit2VolC = peakLock[d.ch2] ? ins2.fm.volC() : 0;
                int limit2VolM = peakLock[d.ch2] ? ins2.fm.volM() : 0;

                switch (d.conn) {
                // FM/FM
                case 0:
                    slideCarrierVolumeUp(d.ch1, slide, limit1VolC);
                    break;
                // FM/AM
                case 1:
                    slideCarrierVolumeUp(d.ch1, slide, limit1VolC);
                    slideModulatorVolumeUp(d.ch2, slide, limit2VolM);
                    break;
                // AM/FM
                case 2:
                    slideCarrierVolumeUp(d.ch1, slide, limit1VolC);
                    slideCarrierVolumeUp(d.ch2, slide, limit2VolC);
                    break;
                // AM/AM
                case 3:
                    slideCarrierVolumeUp(d.ch1, slide, limit1VolC);
                    slideModulatorVolumeUp(d.ch1, slide, limit1VolM);
                    slideModulatorVolumeUp(d.ch2, slide, limit2VolM);
                    break;
                }
            }
            break;

        case 1:
            slideCarrierVolumeUp(chan, slide, limit1);
            break;

        case 2:
            slideModulatorVolumeUp(chan, slide, limit2);
            break;

        case 3:
            slideCarrierVolumeUp(chan, slide, limit1);
            slideModulatorVolumeUp(chan, slide, limit2);
            break;
        }
    }

    private void slideCarrierVolumeDown(int chan, int slide) {
        int volC = fmparTable[chan].volC();
        int newvolC = volC + slide <= 63 ? volC + slide : 63;

        setInsVolume(BYTE_NULL, newvolC, chan);
    }

    private void slideModulatorVolumeDown(int chan, int slide) {
        int volM = fmparTable[chan].volM();
        int newvolM = volM + slide <= 63 ? volM + slide : 63;

        setInsVolume(newvolM, BYTE_NULL, chan);
    }

    private void slideVolumeDown(int chan, int slide) {
        FourOpData d = get4opData(chan);

        switch (volslideType[chan]) {
        case 0:
            if (!is4opVolValidChan(chan)) {
                InstrData i = getInstrDataByCh(chan);
                if (i == null) i = zeroins;

                slideCarrierVolumeDown(chan, slide);

                if (i.fm.connect() != 0 || (percussionMode && (chan >= 16))) { // in [17..20]
                    slideModulatorVolumeDown(chan, slide);
                }
            } else {
                switch (d.conn) {
                // FM/FM
                case 0:
                    slideCarrierVolumeDown(d.ch1, slide);
                    break;
                // FM/AM
                case 1:
                    slideCarrierVolumeDown(d.ch1, slide);
                    slideModulatorVolumeDown(d.ch2, slide);
                    break;
                // AM/FM
                case 2:
                    slideCarrierVolumeDown(d.ch1, slide);
                    slideCarrierVolumeDown(d.ch2, slide);
                    break;
                // AM/AM
                case 3:
                    slideCarrierVolumeDown(d.ch1, slide);
                    slideModulatorVolumeDown(d.ch1, slide);
                    slideModulatorVolumeDown(d.ch2, slide);
                    break;
                }
            }
            break;

        case 1:
            slideCarrierVolumeDown(chan, slide);
            break;

        case 2:
            slideModulatorVolumeDown(chan, slide);
            break;

        case 3:
            slideCarrierVolumeDown(chan, slide);
            slideModulatorVolumeDown(chan, slide);
            break;
        }
    }

    private void volumeSlide(int chan, int upSpeed, int downSpeed) {
        if (upSpeed != 0) {
            slideVolumeUp(chan, upSpeed);
        } else if (downSpeed != 0) {
            slideVolumeDown(chan, downSpeed);
        }
    }

    private void globalVolumeSlide(int upSpeed, int downSpeed) {
        if (upSpeed != BYTE_NULL) {
            globalVolume = clampMax(globalVolume + upSpeed, 63);
        }

        if (downSpeed != BYTE_NULL) {
            if (globalVolume >= downSpeed) {
                globalVolume -= downSpeed;
            } else {
                globalVolume = 0;
            }
        }

        setGlobalVolume();
    }

    private static final int[] arpggState = { 1, 2, 0 };

    private void arpeggio(int slot, int chan) {
        int freq;

        switch (arpggTableState[slot][chan]) {
        case 0: freq = nFreq(arpggTableNote[slot][chan] - 1); break;
        case 1: freq = nFreq(arpggTableNote[slot][chan] - 1 + arpggTableAdd1[slot][chan]); break;
        case 2: freq = nFreq(arpggTableNote[slot][chan] - 1 + arpggTableAdd2[slot][chan]); break;
        default: freq = 0;
        }

        arpggTableState[slot][chan] = arpggState[arpggTableState[slot][chan]];
        changeFrequency(chan, (freq + getInstrFineTune(eventTable[chan].instrDef)) & 0xffff);
    }

    private void vibrato(int slot, int chan) {
        int freq, slide;
        int direction;

        freq = freqTable[chan];

        vibrTablePos[slot][chan] = (vibrTablePos[slot][chan] + vibrTableSpeed[slot][chan]) & 0xff;
        slide = calcVibratoShift(vibrTableDepth[slot][chan], vibrTablePos[slot][chan]);
        direction = vibrTablePos[slot][chan] & 0x20;

        if (direction == 0) {
            portamentoDown(chan, slide, nFreq(0));
        } else {
            portamentoUp(chan, slide, nFreq(12 * 8 + 1));
        }

        freqTable[chan] = freq;
    }

    private void tremolo(int slot, int chan) {
        int slide;
        int direction;

        int volM = fmparTable[chan].volM();
        int volC = fmparTable[chan].volC();

        tremTablePos[slot][chan] = (tremTablePos[slot][chan] + tremTableSpeed[slot][chan]) & 0xff;
        slide = calcVibratoShift(tremTableDepth[slot][chan], tremTablePos[slot][chan]);
        direction = tremTablePos[slot][chan] & 0x20;

        if (direction == 0) {
            slideVolumeDown(chan, slide);
        } else {
            slideVolumeUp(chan, slide);
        }

        fmparTable[chan].volM(volM);
        fmparTable[chan].volC(volC);
    }

    private int chanvol(int chan) {
        InstrData instr = getInstrDataByCh(chan);
        if (instr == null) instr = zeroins;

        if (instr.fm.connect() == 0) {
            return 63 - fmparTable[chan].volC();
        } else {
            return 63 - (fmparTable[chan].volM() + fmparTable[chan].volC()) / 2;
        }
    }

    private void updateEffectsSlot(int slot, int chan) {
        int def = effectTableDef[slot][chan];
        int val = effectTableVal[slot][chan];

        switch (def) {
        case ef_Arpeggio:
            if (val == 0) break;

            arpeggio(slot, chan);
            break;

        case ef_ArpggVSlide:
            volumeSlide(chan, val / 16, val % 16);
            arpeggio(slot, chan);
            break;

        case ef_ArpggVSlideFine:
            arpeggio(slot, chan);
            break;

        case ef_FSlideUp:
            portamentoUp(chan, val, nFreq(12 * 8 + 1));
            break;

        case ef_FSlideDown:
            portamentoDown(chan, val, nFreq(0));
            break;

        case ef_FSlideUpVSlide:
            portamentoUp(chan, fslideTable[slot][chan], nFreq(12 * 8 + 1));
            volumeSlide(chan, val / 16, val % 16);
            break;

        case ef_FSlUpVSlF:
            portamentoUp(chan, fslideTable[slot][chan], nFreq(12 * 8 + 1));
            break;

        case ef_FSlideDownVSlide:
            portamentoDown(chan, fslideTable[slot][chan], nFreq(0));
            volumeSlide(chan, val / 16, val % 16);
            break;

        case ef_FSlDownVSlF:
            portamentoDown(chan, fslideTable[slot][chan], nFreq(0));
            break;

        case ef_FSlUpFineVSlide:
        case ef_FSlDownFineVSlide:
            volumeSlide(chan, val / 16, val % 16);
            break;

        case ef_TonePortamento:
            tonePortamento(slot, chan);
            break;

        case ef_TPortamVolSlide:
            volumeSlide(chan, val / 16, val % 16);
            tonePortamento(slot, chan);
            break;

        case ef_TPortamVSlideFine:
            tonePortamento(slot, chan);
            break;

        case ef_Vibrato:
            if (!vibrTableFine[slot][chan]) {
                vibrato(slot, chan);
            }
            break;

        case ef_Tremolo:
            if (!tremTableFine[slot][chan]) {
                tremolo(slot, chan);
            }
            break;

        case ef_VibratoVolSlide:
            volumeSlide(chan, val / 16, val % 16);
            if (!vibrTableFine[slot][chan]) {
                vibrato(slot, chan);
            }
            break;

        case ef_VibratoVSlideFine:
            if (!vibrTableFine[slot][chan]) {
                vibrato(slot, chan);
            }
            break;

        case ef_VolSlide:
            volumeSlide(chan, val / 16, val % 16);
            break;

        case ef_RetrigNote:
            if (retrigTable[slot][chan] >= val) {
                retrigTable[slot][chan] = 0;
                outputNote(eventTable[chan].note, eventTable[chan].instrDef, chan, true, true);
            } else {
                retrigTable[slot][chan]++;
            }
            break;

        case ef_MultiRetrigNote:
            if (retrigTable[slot][chan] >= val / 16) {
                switch (val % 16) {
                case 0: break;
                case 8: break;

                case 1: slideVolumeDown(chan, 1); break;
                case 2: slideVolumeDown(chan, 2); break;
                case 3: slideVolumeDown(chan, 4); break;
                case 4: slideVolumeDown(chan, 8); break;
                case 5: slideVolumeDown(chan, 16); break;

                case 9: slideVolumeUp(chan, 1); break;
                case 10: slideVolumeUp(chan, 2); break;
                case 11: slideVolumeUp(chan, 4); break;
                case 12: slideVolumeUp(chan, 8); break;
                case 13: slideVolumeUp(chan, 16); break;

                case 6: slideVolumeDown(chan, chanvol(chan) - chanvol(chan) * 2 / 3); break;

                case 7: slideVolumeDown(chan, chanvol(chan) - chanvol(chan) * 1 / 2); break;

                case 14: slideVolumeUp(chan, clampMax(chanvol(chan) * 3 / 2 - chanvol(chan), 63)); break;

                case 15: slideVolumeUp(chan, clampMax(chanvol(chan) * 2 - chanvol(chan), 63)); break;
                }

                retrigTable[slot][chan] = 0;
                outputNote(eventTable[chan].note, eventTable[chan].instrDef, chan, true, true);
            } else {
                retrigTable[slot][chan]++;
            }
            break;

        case ef_Tremor:
            if (tremorTablePos[slot][chan] >= 0) {
                if ((tremorTablePos[slot][chan] + 1) <= val / 16) {
                    tremorTablePos[slot][chan]++;
                } else {
                    slideVolumeDown(chan, 63);
                    tremorTablePos[slot][chan] = -1;
                }
            } else {
                if ((tremorTablePos[slot][chan] - 1) >= -(val % 16)) {
                    tremorTablePos[slot][chan]--;
                } else {
                    setInsVolume(tremorTableVolM[slot][chan], tremorTableVolC[slot][chan], chan);
                    tremorTablePos[slot][chan] = 1;
                }
            }
            break;

        case ef_Extended2:
            switch (val / 16) {
            case ef_ex2_NoteDelay:
                if (notedelTable[chan] == 0) {
                    notedelTable[chan] = BYTE_NULL;
                    outputNote(eventTable[chan].note, eventTable[chan].instrDef, chan, true, true);
                } else if (notedelTable[chan] != BYTE_NULL) {
                    notedelTable[chan]--;
                }
                break;
            case ef_ex2_NoteCut:
                if (notecutTable[chan] == 0) {
                    notecutTable[chan] = BYTE_NULL;
                    keyOff(chan);
                } else if (notecutTable[chan] != BYTE_NULL) {
                    notecutTable[chan]--;
                }
                break;
            case ef_ex2_GlVolSlideUp: globalVolumeSlide(val & 0xf, BYTE_NULL); break;
            case ef_ex2_GlVolSlideDn: globalVolumeSlide(BYTE_NULL, val & 0xf); break;
            }
            break;
        }
    }

    private void updateEffects() {
        for (int chan = 0; chan < nmTracks; chan++) {
            updateEffectsSlot(0, chan);
            updateEffectsSlot(1, chan);
        }
    }

    private void updateFineEffects(int slot, int chan) {
        int def = effectTableDef[slot][chan];
        int val = effectTableVal[slot][chan];

        switch (def) {
        case ef_ArpggVSlideFine:    volumeSlide(chan, val / 16, val % 16); break;
        case ef_FSlideUpFine:       portamentoUp(chan, val, nFreq(12 * 8 + 1)); break;
        case ef_FSlideDownFine:     portamentoDown(chan, val, nFreq(0)); break;
        case ef_FSlUpVSlF:          volumeSlide(chan, val / 16, val % 16); break;
        case ef_FSlDownVSlF:        volumeSlide(chan, val / 16, val % 16); break;
        case ef_FSlUpFineVSlide:    portamentoUp(chan, fslideTable[slot][chan], nFreq(12 * 8 + 1)); break;
        case ef_FSlDownFineVSlide:  portamentoDown(chan, fslideTable[slot][chan], nFreq(0)); break;

        case ef_FSlUpFineVSlF:
            portamentoUp(chan, fslideTable[slot][chan], nFreq(12 * 8 + 1));
            volumeSlide(chan, val / 16, val % 16);
            break;

        case ef_FSlDownFineVSlF:
            portamentoDown(chan, fslideTable[slot][chan], nFreq(0));
            volumeSlide(chan, val / 16, val % 16);
            break;

        case ef_TPortamVSlideFine:  volumeSlide(chan, val / 16, val % 16); break;
        case ef_Vibrato:            if (vibrTableFine[slot][chan]) vibrato(slot, chan); break;
        case ef_Tremolo:            if (tremTableFine[slot][chan]) tremolo(slot, chan); break;
        case ef_VibratoVolSlide:    if (vibrTableFine[slot][chan]) vibrato(slot, chan); break;

        case ef_VibratoVSlideFine:
            volumeSlide(chan, val / 16, val % 16);
            if (vibrTableFine[slot][chan]) {
                vibrato(slot, chan);
            }
            break;

        case ef_VolSlideFine:       volumeSlide(chan, val / 16, val % 16); break;

        case ef_Extended2:
            switch (val / 16) {
            case ef_ex2_GlVolSlideUpF: globalVolumeSlide(val & 0xf, BYTE_NULL); break;
            case ef_ex2_GlVolSlideDnF: globalVolumeSlide(BYTE_NULL, val & 0xf); break;
            }
            break;
        }
    }

    private void updateExtraFineEffectsSlot(int slot, int chan) {
        int def = effectTableDef[slot][chan];
        int val = effectTableVal[slot][chan];

        switch (def) {
        case ef_Extended2:
            switch (val / 16) {
            case ef_ex2_GlVolSldUpXF:  globalVolumeSlide(val & 0xf, BYTE_NULL); break;
            case ef_ex2_GlVolSldDnXF:  globalVolumeSlide(BYTE_NULL, val & 0xf); break;
            case ef_ex2_VolSlideUpXF:  volumeSlide(chan, val & 0xf, 0); break;
            case ef_ex2_VolSlideDnXF:  volumeSlide(chan, 0, val & 0xf); break;
            case ef_ex2_FreqSlideUpXF: portamentoUp(chan, val & 0xf, nFreq(12 * 8 + 1)); break;
            case ef_ex2_FreqSlideDnXF: portamentoDown(chan, val & 0xf, nFreq(0)); break;
            }
            break;

        case ef_GlobalFreqSlideUpXF: portamentoUp(chan, val, nFreq(12 * 8 + 1)); break;
        case ef_GlobalFreqSlideDnXF: portamentoDown(chan, val, nFreq(0)); break;
        case ef_ExtraFineArpeggio:   arpeggio(slot, chan); break;
        case ef_ExtraFineVibrato:    if (!vibrTableFine[slot][chan]) vibrato(slot, chan); break;
        case ef_ExtraFineTremolo:    if (!tremTableFine[slot][chan]) tremolo(slot, chan); break;
        }
    }

    private void updateExtraFineEffects() {
        for (int chan = 0; chan < nmTracks; chan++) {
            updateExtraFineEffectsSlot(0, chan);
            updateExtraFineEffectsSlot(1, chan);
        }
    }

    private void setCurrentOrder(int newOrder) {
        currentOrder = newOrder < 0x80 ? newOrder : 0;

        if (patternOrder[currentOrder] < 0x80) {
            return;
        }

        // protect from circular jump to jump order command:
        // if after 128 attempts of order jump we still land on jump command, quit
        int i = 0;
        do {
            if (patternOrder[currentOrder] > 0x7f) {
                int oldOrder = currentOrder;
                currentOrder = patternOrder[currentOrder] - 0x80;
                if (currentOrder <= oldOrder) {
                    songend = true;
                }
            }
            i++;
        } while (i < 128 && patternOrder[currentOrder] > 0x7f);

        if (i >= 128) {
            songend = true;
            a2tStop();
        }
    }

    private int calcFollowingOrder(int order) {
        int result = -1;
        int index = order;
        int jumpCount = 0;

        do {
            if (patternOrder[index] < 0x80) {
                result = index;
            } else {
                index = patternOrder[index] - 0x80;
                jumpCount++;
            }
        } while (!((jumpCount > 0x7f) || (result != -1)));

        return result;
    }

    private void updateSongPosition() {
        if ((currentLine < pattLen - 1) && !patternBreak) {
            currentLine++;
        } else {
            boolean doPatternLoop = patternBreak && ((nextLine & 0xf0) == pattern_loop_flag);
            boolean doPositionJump = patternBreak && ((nextLine & 0xf0) == pattern_break_flag);

            if (doPatternLoop) {
                // ZCx, ZDx - loop back
                int chan = nextLine - pattern_loop_flag;
                nextLine = loopbckTable[chan];

                if (loopTable[chan][currentLine] != 0) {
                    loopTable[chan][currentLine]--;
                }
            } else {
                for (int[] lt : loopTable) {
                    Arrays.fill(lt, BYTE_NULL);
                }
                Arrays.fill(loopbckTable, BYTE_NULL);

                if (doPositionJump) {
                    // Bxx - order position jump
                    int oldOrder = currentOrder;

                    int chan = nextLine - pattern_break_flag;
                    int slot = eventTable[chan].effDef[0] == ef_PositionJump ? 0 : 1;
                    int val = eventTable[chan].effVal[slot];

                    setCurrentOrder(val);

                    if (currentOrder <= oldOrder) {
                        songend = true;
                    }
                    patternBreak = false;
                } else {
                    int newOrder = currentOrder < 0x7f ? currentOrder + 1 : 0;
                    setCurrentOrder(newOrder);
                }
            }

            if (patternOrder[currentOrder] > 0x7f) {
                return;
            }

            currentPattern = patternOrder[currentOrder];
            if (!patternBreak) {
                currentLine = 0;
            } else {
                patternBreak = false;
                currentLine = nextLine;
            }
        }

        for (int chan = 0; chan < nmTracks; chan++) {
            glfsldTableDef[0][chan] = 0;
            glfsldTableVal[0][chan] = 0;
            glfsldTableDef[1][chan] = 0;
            glfsldTableVal[1][chan] = 0;
        }

        if (speedUpdate && currentLine == 0 && currentOrder == calcFollowingOrder(0)) {
            tempo = songTempo;
            speed = songSpeed;
            updateTimer(tempo);
        }
    }

    private void pollProc() {
        if (patternDelay) {
            updateEffects();
            if (tickD > 1) {
                tickD--;
            } else {
                patternDelay = false;
            }
        } else {
            if (ticks == 0) {
                playLine();
                ticks = speed;
                updateSongPosition();
            }
            updateEffects();
            ticks--;
        }

        tickXF++;
        if (tickXF % 4 == 0) {
            updateExtraFineEffects();
            tickXF -= 4;
        }
    }

    private void macroPollProc() {
        for (int chan = 0; chan < 20; chan++) {
            int finishedFlag = keyoffLoop[chan] ? IDLE : FINISHED;

            MacroTable mt = macroTable[chan];
            FmregTable rt = getFmregTable(mt.fmregIns);

            boolean forceMacroKeyon = false;

            if (rt != null && rt.length != 0) {
                if (mt.fmregDuration > 1) {
                    mt.fmregDuration--;
                } else {
                    if (mt.fmregPos <= rt.length) {
                        if (rt.loopBegin != 0 && rt.loopLength != 0) {
                            if (mt.fmregPos == rt.loopBegin + rt.loopLength - 1) {
                                mt.fmregPos = rt.loopBegin;
                            } else {
                                if (mt.fmregPos < rt.length) {
                                    mt.fmregPos++;
                                } else {
                                    mt.fmregPos = finishedFlag;
                                }
                            }
                        } else {
                            if (mt.fmregPos < rt.length) {
                                mt.fmregPos++;
                            } else {
                                mt.fmregPos = finishedFlag;
                            }
                        }
                    } else {
                        mt.fmregPos = finishedFlag;
                    }

                    if (((freqTable[chan] | 0x2000) == freqTable[chan]) &&
                            (rt.keyoffPos != 0) &&
                            (mt.fmregPos >= rt.keyoffPos)) {
                        mt.fmregPos = IDLE;
                    } else {
                        if (((freqTable[chan] | 0x2000) != freqTable[chan]) &&
                                (mt.fmregPos != 0) && (rt.keyoffPos != 0) &&
                                ((mt.fmregPos < rt.keyoffPos) || (mt.fmregPos == IDLE))) {
                            mt.fmregPos = rt.keyoffPos;
                        }
                    }

                    if (mt.fmregPos != 0 && mt.fmregPos != IDLE && mt.fmregPos != FINISHED) {
                        mt.fmregDuration = rt.data[mt.fmregPos - 1].duration;

                        if (mt.fmregDuration != 0) {
                            RegTableDef d = rt.data[mt.fmregPos - 1];
                            long disabled = instruments[mt.fmregIns - 1].disFmregCols;

                            // force KEY-ON with missing ADSR instrument data
                            forceMacroKeyon = false;
                            if (mt.fmregPos == 1) {
                                long adsrDisabled = disabled & ((1 << 0) | (1 << 1) | (1 << 2) | (1 << 3) |
                                        (1 << 12) | (1 << 13) | (1 << 14) | (1 << 15));
                                if (isInsAdsrDataEmpty(voiceTable[chan]) && adsrDisabled == 0) {
                                    forceMacroKeyon = true;
                                }
                            }

                            for (int bit = 0; bit < 28; bit++) {
                                if ((disabled & (1L << bit)) != 0) continue;

                                switch (bit) {
                                case 0:  fmparTable[chan].attckM(d.fm.attckM()); break;
                                case 1:  fmparTable[chan].decM(d.fm.decM()); break;
                                case 2:  fmparTable[chan].sustnM(d.fm.sustnM()); break;
                                case 3:  fmparTable[chan].relM(d.fm.relM()); break;
                                case 4:  fmparTable[chan].wformM(d.fm.wformM()); break;
                                case 5:  setInsVolume(63 - d.fm.volM(), BYTE_NULL, chan); break;
                                case 6:  fmparTable[chan].kslM(d.fm.kslM()); break;
                                case 7:  fmparTable[chan].multipM(d.fm.multipM()); break;
                                case 8:  fmparTable[chan].tremM(d.fm.tremM()); break;
                                case 9:  fmparTable[chan].vibrM(d.fm.vibrM()); break;
                                case 10: fmparTable[chan].ksrM(d.fm.ksrM()); break;
                                case 11: fmparTable[chan].sustM(d.fm.sustM()); break;
                                case 12: fmparTable[chan].attckC(d.fm.attckC()); break;
                                case 13: fmparTable[chan].decC(d.fm.decC()); break;
                                case 14: fmparTable[chan].sustnC(d.fm.sustnC()); break;
                                case 15: fmparTable[chan].relC(d.fm.relC()); break;
                                case 16: fmparTable[chan].wformC(d.fm.wformC()); break;
                                case 17: setInsVolume(BYTE_NULL, 63 - d.fm.volC(), chan); break;
                                case 18: fmparTable[chan].kslC(d.fm.kslC()); break;
                                case 19: fmparTable[chan].multipC(d.fm.multipC()); break;
                                case 20: fmparTable[chan].tremC(d.fm.tremC()); break;
                                case 21: fmparTable[chan].vibrC(d.fm.vibrC()); break;
                                case 22: fmparTable[chan].ksrC(d.fm.ksrC()); break;
                                case 23: fmparTable[chan].sustC(d.fm.sustC()); break;
                                case 24: fmparTable[chan].connect(d.fm.connect()); break;
                                case 25: fmparTable[chan].feedb(d.fm.feedb()); break;
                                case 27: if (!panLock[chan]) panningTable[chan] = d.panning; break;
                                }
                            }

                            updateModulatorAdsrw(chan);
                            updateCarrierAdsrw(chan);
                            updateFmpar(chan);

                            int macroFlags = d.fm.data[10];

                            if (forceMacroKeyon || (macroFlags & 0x80) != 0) { // MACRO_NOTE_RETRIG_FLAG
                                if (!(is4opChan(chan) && is4opChanHi(chan))) {
                                    outputNote(eventTable[chan].note,
                                            eventTable[chan].instrDef, chan, false, true);
                                    if (is4opChan(chan) && is4opChanLo(chan)) {
                                        initMacroTable(chan - 1, 0, voiceTable[chan - 1], 0);
                                    }
                                }
                            } else if ((macroFlags & 0x40) != 0) { // MACRO_ENVELOPE_RESTART_FLAG
                                keyOn(chan);
                                changeFreq(chan, freqTable[chan]);
                            } else if ((macroFlags & 0x20) != 0) { // MACRO_ZERO_FREQ_FLAG
                                if (freqTable[chan] != 0) {
                                    zeroFqTable[chan] = freqTable[chan];
                                    freqTable[chan] &= ~0x1fff;
                                    changeFreq(chan, freqTable[chan]);
                                } else if (zeroFqTable[chan] != 0) {
                                    freqTable[chan] = zeroFqTable[chan];
                                    zeroFqTable[chan] = 0;
                                    changeFreq(chan, freqTable[chan]);
                                }
                            }

                            int freqSlide = d.freqSlide;

                            if ((disabled & (1L << 26)) == 0) {
                                if (freqSlide > 0) {
                                    portamentoUp(chan, freqSlide, nFreq(12 * 8 + 1));
                                } else if (freqSlide < 0) {
                                    portamentoDown(chan, Math.abs(freqSlide), nFreq(0));
                                }
                            }
                        }
                    }
                }
            }

            ArpeggioTable at = getArpeggioTable(mt.arpgTable);

            if (at != null && at.length != 0 && at.speed != 0) {
                if (mt.arpgCount == at.speed) {
                    mt.arpgCount = 1;

                    if (mt.arpgPos <= at.length) {
                        if ((at.loopBegin != 0) && (at.loopLength != 0)) {
                            if (mt.arpgPos == at.loopBegin + (at.loopLength - 1)) {
                                mt.arpgPos = at.loopBegin;
                            } else {
                                if (mt.arpgPos < at.length) {
                                    mt.arpgPos++;
                                } else {
                                    mt.arpgPos = finishedFlag;
                                }
                            }
                        } else {
                            if (mt.arpgPos < at.length) {
                                mt.arpgPos++;
                            } else {
                                mt.arpgPos = finishedFlag;
                            }
                        }
                    } else {
                        mt.arpgPos = finishedFlag;
                    }

                    if (((freqTable[chan] | 0x2000) == freqTable[chan]) &&
                            (at.keyoffPos != 0) &&
                            (mt.arpgPos >= at.keyoffPos)) {
                        mt.arpgPos = IDLE;
                    } else {
                        if (((freqTable[chan] | 0x2000) != freqTable[chan]) &&
                                (at.keyoffPos != 0) &&
                                ((mt.arpgPos < at.keyoffPos) || (mt.arpgPos == IDLE))) {
                            mt.arpgPos = at.keyoffPos;
                        }
                    }

                    if ((mt.arpgPos != 0) &&
                            (mt.arpgPos != IDLE) && (mt.arpgPos != FINISHED)) {
                        int fineTune = getInstrFineTune(eventTable[chan].instrDef);
                        int d = at.data[mt.arpgPos - 1];

                        if (d == 0) {
                            changeFrequency(chan, (nFreq(mt.arpgNote - 1) + fineTune) & 0xffff);
                        } else if (d <= 96) {
                            // 1 - 96 (note: data[arpgPos], not [arpgPos - 1], as in adplug)
                            int idx = mt.arpgPos < 255 ? at.data[mt.arpgPos] : 0;
                            changeFrequency(chan, (nFreq(clampMax(mt.arpgNote + idx, 97) - 1) + fineTune) & 0xffff);
                        } else if (d >= 0x80 && d <= 0x80 + 12 * 8 + 1) {
                            changeFrequency(chan, (nFreq(at.data[mt.arpgPos - 1] - 0x80 - 1) + fineTune) & 0xffff);
                        }
                    }
                } else {
                    mt.arpgCount++;
                }
            }

            VibratoTable vt = getVibratoTable(mt.vibTable);

            if (vt != null && vt.length != 0 && vt.speed != 0 && !mt.vibPaused) {
                if (mt.vibCount == vt.speed) {
                    if (mt.vibDelay != 0) {
                        mt.vibDelay--;
                    } else {
                        mt.vibCount = 1;

                        if (mt.vibPos <= vt.length) {
                            if ((vt.loopBegin != 0) && (vt.loopLength != 0)) {
                                if (mt.vibPos == vt.loopBegin + (vt.loopLength - 1)) {
                                    mt.vibPos = vt.loopBegin;
                                } else {
                                    if (mt.vibPos < vt.length) {
                                        mt.vibPos++;
                                    } else {
                                        mt.vibPos = finishedFlag;
                                    }
                                }
                            } else {
                                if (mt.vibPos < vt.length) {
                                    mt.vibPos++;
                                } else {
                                    mt.vibPos = finishedFlag;
                                }
                            }
                        } else {
                            mt.vibPos = finishedFlag;
                        }

                        if (((freqTable[chan] | 0x2000) == freqTable[chan]) &&
                                (vt.keyoffPos != 0) &&
                                (mt.vibPos >= vt.keyoffPos)) {
                            mt.vibPos = IDLE;
                        } else {
                            if (((freqTable[chan] | 0x2000) != freqTable[chan]) &&
                                    (mt.vibPos != 0) && (vt.keyoffPos != 0) &&
                                    ((mt.vibPos < vt.keyoffPos) || (mt.vibPos == IDLE))) {
                                mt.vibPos = vt.keyoffPos;
                            }
                        }

                        if ((mt.vibPos != 0) &&
                                (mt.vibPos != IDLE) && (mt.vibPos != FINISHED)) {
                            // note: depth reads data[vibPos], sign checks [vibPos - 1], as in adplug
                            int cur = mt.vibPos < 255 ? vt.data[mt.vibPos] : 0;
                            if (vt.data[mt.vibPos - 1] > 0) {
                                macroVibratoPortaUp(chan, cur);
                            } else if (vt.data[mt.vibPos - 1] < 0) {
                                macroVibratoPortaDown(chan, Math.abs(cur));
                            } else {
                                changeFreq(chan, mt.vibFreq);
                            }
                        }
                    }
                } else {
                    mt.vibCount++;
                }
            }
        }
    }

    private void newtimer() {
        if ((ticklooper == 0) && irqMode) {
            pollProc();
            if (IRQ_freq != tempo * macroSpeedupEff()) {
                IRQ_freq = (tempo < 18 ? 18 : tempo) * macroSpeedupEff();
            }
        }

        if ((macroTicklooper == 0) && irqMode) {
            macroPollProc();
        }

        ticklooper++;
        if (ticklooper >= IRQ_freq / tempo) {
            ticklooper = 0;
        }

        macroTicklooper++;
        if (macroTicklooper >= IRQ_freq / (tempo * macroSpeedupEff())) {
            macroTicklooper = 0;
        }
    }

    private void initBuffers() {
        for (int i = 0; i < 20; i++) {
            Arrays.fill(fmparTable[i].data, 0);
            eventTable[i] = new A2Event();
            macroTable[i] = new MacroTable();
        }
        Arrays.fill(modulatorVol, 0);
        Arrays.fill(carrierVol, 0);
        Arrays.fill(voiceTable, 0);
        Arrays.fill(freqTable, 0);
        Arrays.fill(zeroFqTable, 0);
        Arrays.fill(portaFKTable, false);
        Arrays.fill(panningTable, 0);
        Arrays.fill(ftuneTable, 0);
        Arrays.fill(keyoffLoop, false);
        Arrays.fill(resetChan, false);
        Arrays.fill(vol4opLock, false);
        for (int slot = 0; slot < 2; slot++) {
            Arrays.fill(effectTableDef[slot], 0);
            Arrays.fill(effectTableVal[slot], 0);
            Arrays.fill(fslideTable[slot], 0);
            Arrays.fill(glfsldTableDef[slot], 0);
            Arrays.fill(glfsldTableVal[slot], 0);
            Arrays.fill(portaTableFreq[slot], 0);
            Arrays.fill(portaTableSpeed[slot], 0);
            Arrays.fill(arpggTableState[slot], 0);
            Arrays.fill(arpggTableNote[slot], 0);
            Arrays.fill(arpggTableAdd1[slot], 0);
            Arrays.fill(arpggTableAdd2[slot], 0);
            Arrays.fill(vibrTablePos[slot], 0);
            Arrays.fill(vibrTableSpeed[slot], 0);
            Arrays.fill(vibrTableDepth[slot], 0);
            Arrays.fill(vibrTableFine[slot], false);
            Arrays.fill(tremTablePos[slot], 0);
            Arrays.fill(tremTableSpeed[slot], 0);
            Arrays.fill(tremTableDepth[slot], 0);
            Arrays.fill(tremTableFine[slot], false);
            Arrays.fill(retrigTable[slot], 0);
            Arrays.fill(tremorTablePos[slot], 0);
            Arrays.fill(tremorTableVolM[slot], 0);
            Arrays.fill(tremorTableVolC[slot], 0);
            Arrays.fill(lastEffectDef[slot], 0);
            Arrays.fill(lastEffectVal[slot], 0);
        }

        if (!lockvol) {
            Arrays.fill(volumeLock, false);
        } else {
            for (int i = 0; i < 20; i++) {
                volumeLock[i] = ((lockFlags[i] >> 4) & 1) != 0;
            }
        }

        if (!panlock) {
            Arrays.fill(panningTable, 0);
        } else {
            for (int i = 0; i < 20; i++) {
                panningTable[i] = lockFlags[i] & 3;
            }
        }

        if (!lockVP) {
            Arrays.fill(peakLock, false);
        } else {
            for (int i = 0; i < 20; i++) {
                peakLock[i] = ((lockFlags[i] >> 5) & 1) != 0;
            }
        }

        int[] _4op_main_chan = { 1, 3, 5, 10, 12, 14 }; // 0-based

        Arrays.fill(vol4opLock, false);
        for (int i = 0; i < 6; i++) {
            vol4opLock[_4op_main_chan[i]] =
                    (lockFlags[_4op_main_chan[i]] | 0x40) == lockFlags[_4op_main_chan[i]];
            vol4opLock[_4op_main_chan[i] - 1] =
                    (lockFlags[_4op_main_chan[i] - 1] | 0x40) == lockFlags[_4op_main_chan[i] - 1];
        }

        for (int i = 0; i < 20; i++) {
            volslideType[i] = (lockFlags[i] >> 2) & 3;
        }

        Arrays.fill(notedelTable, BYTE_NULL);
        Arrays.fill(notecutTable, BYTE_NULL);
        Arrays.fill(loopbckTable, BYTE_NULL);
        for (int[] lt : loopTable) {
            Arrays.fill(lt, BYTE_NULL);
        }
    }

    private void initPlayer() {
        opl2out(0x01, 0);

        for (int i = 0; i < 18; i++) {
            opl2out(0xb0 + regoffsN(i), 0);
        }

        for (int i = 0x80; i <= 0x8d; i++) {
            opl2out(i, BYTE_NULL);
        }

        for (int i = 0x90; i <= 0x95; i++) {
            opl2out(i, BYTE_NULL);
        }

        miscRegister = (tremoloDepth << 7) +
                (vibratoDepth << 6) +
                ((percussionMode ? 1 : 0) << 5);

        opl2out(0x01, 0x20);
        opl2out(0x08, 0x40);
        opl3exp(0x0105);
        opl3exp(0x04 + (flag4op << 8));

        keyOff(16);
        keyOff(17);
        opl2out(0xbd, miscRegister);

        initBuffers();

        currentTremoloDepth = tremoloDepth;
        currentVibratoDepth = vibratoDepth;
        globalVolume = 63;

        vibtremSpeedFactor = def_vibtrem_speed_factor;
        vibtremTableSize = def_vibtrem_table_size;
        System.arraycopy(def_vibtrem_table, 0, vibtremTable, 0, 256);

        for (int i = 0; i < 20; i++) {
            arpggTableState[0][i] = 1;
            arpggTableState[1][i] = 1;
            voiceTable[i] = i + 1;
        }
    }

    private void a2tStop() {
        irqMode = false;
        globalVolume = 63;
        currentTremoloDepth = tremoloDepth;
        currentVibratoDepth = vibratoDepth;
        patternBreak = false;
        currentOrder = 0;
        currentPattern = 0;
        currentLine = 0;
        playbackSpeedShift = 0;

        for (int i = 0; i < 20; i++) {
            releaseSustainingSound(i);
        }

        opl2out(0xbd, 0);
        opl3exp(0x0004);
        opl3exp(0x0005);
        lockvol = false;
        panlock = false;
        lockVP = false;
        initBuffers();

        speed = 4;
        updateTimer(50);
    }
}
