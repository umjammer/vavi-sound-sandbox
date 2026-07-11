/*
 * SoundFX Macs Opera CMF Player -- Copyright (c) 2017 Sebastian Kienzl <seb@knzl.de>
 *
 * Part of Adplug - Replayer for many OPL2/OPL3 audio file formats.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioFileFormat.Type;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

/**
 * SoundFX Macs Opera CMF Player.
 * Ported from adplug's cmfmcsop.cpp / cmfmcsop.h by Sebastian Kienzl.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class CmfmcsopPlayer extends Opl3Player {

    private static final int[] fNumbers = {
        0x157, 0x16b, 0x181, 0x198, 0x1b0, 0x1ca, 0x1e5, 0x202, 0x221, 0x242, 0x264, 0x288
    };

    private static final int[] slotRegisterOffsets = {
        0, 1, 2, 3, 4, 5,
        8, 9, 10, 11, 12, 13,
        16, 17, 18, 19, 20, 21
    };

    private static class ChannelSlots {
        final int slotOp1;
        final int slotOp2;
        ChannelSlots(int op1, int op2) {
            this.slotOp1 = op1;
            this.slotOp2 = op2;
        }
    }

    private static final ChannelSlots[] channelSlots = {
        new ChannelSlots(0, 3), new ChannelSlots(1, 4), new ChannelSlots(2, 5),
        new ChannelSlots(6, 9), new ChannelSlots(7, 10), new ChannelSlots(8, 11),
        new ChannelSlots(12, 15), new ChannelSlots(13, 16), new ChannelSlots(14, 17)
    };

    private static final int[] channelSlotsRhythm = {
        -1, -1, -1, -1, -1, -1, 12, 16, 14, 17, 13
    };

    private static final int CHANNEL_RHY_BD = 6;
    private static final int CHANNEL_RHY_SN = 7;
    private static final int CHANNEL_RHY_TOM = 8;
    private static final int CHANNEL_RHY_TC = 9;
    private static final int CHANNEL_RHY_HH = 10;

    private static class SlotSettings {
        int ksl;
        int multiple;
        int attackRate;
        int sustainLevel;
        int egType;
        int decayRate;
        int releaseRate;
        int totalLevel;
        int ampMod;
        int vib;
        int ksr;
        int waveSelect;
    }

    private static class Instrument {
        final SlotSettings[] op = { new SlotSettings(), new SlotSettings() };
        int feedback;
        int connection;
        String name = "";
    }

    private static class NoteEvent {
        int row;
        int col;
        int note;
        int instrument;
        int volume;
        int pitch;
    }

    private float speedRowsPerSec;
    private boolean rhythmMode;
    private boolean songDone;
    private int nrOfPatterns;
    private final int[] patternOrder = new int[99];
    private int nrOfOrders;

    private final List<Instrument> instruments = new ArrayList<>();
    private final List<List<NoteEvent>> patterns = new ArrayList<>();

    private int currentOrderIndex;
    private int currentRow;
    private int currentPatternIndex;

    private final Instrument[] channelCurrentInstrument = new Instrument[11];
    private final int[] current0xBx = new int[9];
    private int current0xBD;

    public CmfmcsopPlayer() {
        super();
    }

    @Override
    public int getTotalMilliseconds() {
        return 0;
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("SoundFX Macs Opera CMF", "cmf");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("CMFMCSOP");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(1024);
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
        return sig[0] == 'A' && sig[1] == '.' && sig[2] == 'H' && sig[3] == '.';
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();
        int read = buf.length;
        if (read < 4) {
            throw new IllegalArgumentException("file too short");
        }

        String signature = new String(buf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII);
        if (!"A.H.".equals(signature)) {
            throw new IllegalArgumentException("invalid signature");
        }

        int offset = 4;
        nrOfOrders = -1;
        for (int i = 0; i < 99; i++) {
            if (offset + 2 > buf.length) throw new IllegalArgumentException("corrupted order list");
            patternOrder[i] = ((buf[offset] & 0xff) << 8) | (buf[offset + 1] & 0xff);
            offset += 2;
            if (patternOrder[i] == 99 && nrOfOrders < 0) {
                nrOfOrders = i;
            }
        }

        if (nrOfOrders == -1) {
            nrOfOrders = 99;
        }

        if (offset + 2 > buf.length) throw new IllegalArgumentException("file corrupted");
        nrOfPatterns = ((buf[offset] & 0xff) << 8) | (buf[offset + 1] & 0xff);
        offset += 2;

        if (offset + 2 > buf.length) throw new IllegalArgumentException("file corrupted");
        int speed = ((buf[offset] & 0xff) << 8) | (buf[offset + 1] & 0xff);
        offset += 2;

        if (speed < 1 || speed > 3) {
            throw new IllegalArgumentException("invalid speed: " + speed);
        }

        speedRowsPerSec = 18.2f / (1 << (speed - 1));

        if (offset + 2 > buf.length) throw new IllegalArgumentException("file corrupted");
        rhythmMode = (((buf[offset] & 0xff) << 8) | (buf[offset + 1] & 0xff)) == 1;
        offset += 2;

        if (offset + 2 > buf.length) throw new IllegalArgumentException("file corrupted");
        int nrOfInstruments = ((buf[offset] & 0xff) << 8) | (buf[offset + 1] & 0xff);
        offset += 2;

        if (!loadInstruments(buf, offset, nrOfInstruments)) {
            throw new IllegalArgumentException("instruments load failed");
        }
        offset += nrOfInstruments * (28 * 2 + 13);

        if (!loadPatterns(buf, offset)) {
            throw new IllegalArgumentException("patterns load failed");
        }

        rewind(0);
    }

    private boolean loadInstruments(byte[] buf, int offset, int nrOfInstruments) {
        if (nrOfInstruments > 0xff) {
            return false;
        }

        for (int i = 0; i < nrOfInstruments; i++) {
            Instrument inst = new Instrument();
            for (int j = 0; j < 28; j++) {
                if (offset + 2 > buf.length) return false;
                int v = ((buf[offset] & 0xff) << 8) | (buf[offset + 1] & 0xff);
                offset += 2;

                switch (j) {
                    case 0 -> inst.op[0].ksl = v;
                    case 1 -> inst.op[0].multiple = v;
                    case 2 -> inst.feedback = v;
                    case 3 -> inst.op[0].attackRate = v;
                    case 4 -> inst.op[0].sustainLevel = v;
                    case 5 -> inst.op[0].egType = v;
                    case 6 -> inst.op[0].decayRate = v;
                    case 7 -> inst.op[0].releaseRate = v;
                    case 8 -> inst.op[0].totalLevel = v;
                    case 9 -> inst.op[0].ampMod = v;
                    case 10 -> inst.op[0].vib = v;
                    case 11 -> inst.op[0].ksr = v;
                    case 12 -> inst.connection = v;

                    case 13 -> inst.op[1].ksl = v;
                    case 14 -> inst.op[1].multiple = v;
                    case 16 -> inst.op[1].attackRate = v;
                    case 17 -> inst.op[1].sustainLevel = v;
                    case 18 -> inst.op[1].egType = v;
                    case 19 -> inst.op[1].decayRate = v;
                    case 20 -> inst.op[1].releaseRate = v;
                    case 21 -> inst.op[1].totalLevel = v;
                    case 22 -> inst.op[1].ampMod = v;
                    case 23 -> inst.op[1].vib = v;
                    case 24 -> inst.op[1].ksr = v;
                    case 26 -> inst.op[0].waveSelect = v;
                    case 27 -> inst.op[1].waveSelect = v;
                }
            }

            if (offset + 13 > buf.length) return false;
            byte[] nameBuf = new byte[13];
            System.arraycopy(buf, offset, nameBuf, 0, 13);
            offset += 13;

            int nameLen = 0;
            while (nameLen < 13 && nameBuf[nameLen] != 0) {
                nameLen++;
            }
            inst.name = new String(nameBuf, 0, nameLen, java.nio.charset.StandardCharsets.US_ASCII).trim();
            instruments.add(inst);
        }

        return true;
    }

    private boolean loadPatterns(byte[] buf, int offset) {
        if (nrOfPatterns > 0xff) {
            return false;
        }

        for (int i = 0; i < nrOfPatterns; i++) {
            List<NoteEvent> pattern = new ArrayList<>();
            while (offset < buf.length) {
                int row = buf[offset] & 0xff;
                offset++;
                if (row == 0xff) {
                    break;
                }

                if (offset + 5 > buf.length) return false;
                NoteEvent n = new NoteEvent();
                n.row = row;
                n.col = buf[offset] & 0xff;
                n.note = buf[offset + 1] & 0xff;
                n.instrument = (buf[offset + 2] & 0xff) - 1;
                n.volume = buf[offset + 3] & 0xff;
                n.pitch = buf[offset + 4] & 0xff;
                offset += 5;

                pattern.add(n);
            }
            patterns.add(pattern);
        }

        return true;
    }

    private boolean isValidChannel(int channelNr) {
        return channelNr >= 0 && ((rhythmMode && channelNr < 11) || (!rhythmMode && channelNr < 9));
    }

    private boolean isRhythmChannel(int channelNr) {
        return rhythmMode && channelNr >= 6;
    }

    private void setSlot(int slotNr, SlotSettings settings) {
        write(0, 0x20 + slotRegisterOffsets[slotNr], (settings.multiple & 0xf) | ((settings.ksr & 1) << 4) | ((settings.egType & 1) << 5) | ((settings.vib & 1) << 6) | ((settings.ampMod & 1) << 7));
        write(0, 0x60 + slotRegisterOffsets[slotNr], ((settings.attackRate & 0xf) << 4) | (settings.decayRate & 0xf));
        write(0, 0x80 + slotRegisterOffsets[slotNr], ((settings.sustainLevel & 0xf) << 4) | (settings.releaseRate & 0xf));
        write(0, 0xe0 + slotRegisterOffsets[slotNr], settings.waveSelect & 3);
    }

    private boolean setInstrument(int channelNr, Instrument inst) {
        if (!isValidChannel(channelNr)) {
            return false;
        }

        if (channelCurrentInstrument[channelNr] != inst) {
            if (!isRhythmChannel(channelNr) || channelNr == CHANNEL_RHY_BD) {
                write(0, 0xc0 + channelNr, ((inst.feedback & 7) << 1) | (1 - (inst.connection & 1)));
                setSlot(channelSlots[channelNr].slotOp1, inst.op[0]);
                setSlot(channelSlots[channelNr].slotOp2, inst.op[1]);
            } else {
                setSlot(channelSlotsRhythm[channelNr], inst.op[0]);
            }
            channelCurrentInstrument[channelNr] = inst;
        }

        return true;
    }

    private int calculateAttenuation(int attenuation, int volumeColumn) {
        if (attenuation < 0) attenuation = 0;
        if (attenuation > 63) attenuation = 63;
        if (volumeColumn < 0) volumeColumn = 0;
        if (volumeColumn > 127) volumeColumn = 127;

        return attenuation + ((63 - attenuation) * (127 - volumeColumn)) / 127;
    }

    private void keyOn(int channelNr) {
        if (!isValidChannel(channelNr)) {
            return;
        }

        if (!isRhythmChannel(channelNr)) {
            current0xBx[channelNr] |= (1 << 5);
            write(0, 0xb0 + channelNr, current0xBx[channelNr]);
        } else {
            current0xBD |= 1 << (10 - channelNr);
            write(0, 0xbd, current0xBD);
        }
    }

    private void keyOff(int channelNr) {
        if (!isValidChannel(channelNr)) {
            return;
        }

        if (!isRhythmChannel(channelNr)) {
            current0xBx[channelNr] &= ~(1 << 5);
            write(0, 0xb0 + channelNr, current0xBx[channelNr]);
        } else {
            current0xBD &= ~(1 << (10 - channelNr));
            write(0, 0xbd, current0xBD);
        }
    }

    private void setAxBx(int channelNr, int Ax, int Bx) {
        if (channelNr < 0 || channelNr >= 8) {
            return;
        }

        write(0, 0xa0 + channelNr, Ax);
        current0xBx[channelNr] = Bx;
        write(0, 0xb0 + channelNr, current0xBx[channelNr]);
    }

    private boolean setNote(int channelNr, int note) {
        if (!isValidChannel(channelNr)) {
            return false;
        }

        if (note < 23 || note >= 120) {
            return false;
        }

        int fNumber = fNumbers[note % 12];
        int Ax = fNumber & 0xff;
        int Bx = ((fNumber >> 8) & 3) | ((note / 12 - 2) << 2);

        if (!isRhythmChannel(channelNr)) {
            setAxBx(channelNr, Ax, Bx);
        } else {
            if (channelNr == CHANNEL_RHY_BD) {
                setAxBx(6, Ax, Bx);
            }
            setAxBx(7, Ax, Bx);
            if (channelNr == CHANNEL_RHY_SN || channelNr == CHANNEL_RHY_TOM) {
                setAxBx(8, Ax, Bx);
            }
        }

        return true;
    }

    private void setVolume(int channelNr, int vol) {
        if (!isValidChannel(channelNr) || channelCurrentInstrument[channelNr] == null) {
            return;
        }

        Instrument inst = channelCurrentInstrument[channelNr];

        if (!isRhythmChannel(channelNr) || channelNr == CHANNEL_RHY_BD) {
            write(0,
                0x40 + slotRegisterOffsets[channelSlots[channelNr].slotOp1],
                ((inst.op[0].ksl & 3) << 6) |
                  (inst.connection == 0 ? calculateAttenuation(inst.op[0].totalLevel, vol) : (inst.op[0].totalLevel & 0x3f))
            );
            write(0,
                0x40 + slotRegisterOffsets[channelSlots[channelNr].slotOp2],
                ((inst.op[1].ksl & 3) << 6) | calculateAttenuation(inst.op[1].totalLevel, vol)
            );
        } else {
            write(0,
                0x40 + slotRegisterOffsets[channelSlotsRhythm[channelNr]],
                ((inst.op[1].ksl & 3) << 6) | calculateAttenuation(inst.op[0].totalLevel, vol)
            );
        }
    }

    private boolean advanceRow() {
        for (;;) {
            currentRow++;
            if (currentRow >= 64) {
                currentRow = 0;
                currentPatternIndex = 0;

                do {
                    currentOrderIndex++;
                    if (currentOrderIndex >= patternOrder.length) {
                        return false;
                    }
                    if (patternOrder[currentOrderIndex] == 99) {
                        return false;
                    }
                } while (patternOrder[currentOrderIndex] >= patterns.size());
            }

            List<NoteEvent> p = patterns.get(patternOrder[currentOrderIndex]);
            if (currentPatternIndex < p.size() && p.get(currentPatternIndex).row == currentRow && p.get(currentPatternIndex).note == 1) {
                currentRow = 64;
            } else {
                break;
            }
        }
        return true;
    }

    private void processNoteEvent(NoteEvent n) {
        int channelNr = n.col;
        if (!isValidChannel(channelNr)) {
            return;
        }

        keyOff(channelNr);

        if (n.note == 4) {
            return;
        }

        if (n.instrument >= 0 && n.instrument < instruments.size()) {
            setInstrument(channelNr, instruments.get(n.instrument));
        }

        setVolume(channelNr, n.volume);

        if (setNote(channelNr, n.note)) {
            keyOn(channelNr);
        }
    }

    @Override
    public boolean update() throws IOException {
        if (songDone) {
            return false;
        }

        int patIdx = patternOrder[currentOrderIndex];
        List<NoteEvent> p = patterns.get(patIdx);

        while (currentPatternIndex < p.size() && p.get(currentPatternIndex).row == currentRow) {
            NoteEvent n = p.get(currentPatternIndex);
            currentPatternIndex++;
            processNoteEvent(n);
        }

        boolean stillPlaying = advanceRow();
        if (!stillPlaying) {
            resetPlayer();
            songDone = true;
        }

        return !songDone;
    }

    private void resetPlayer() {
        currentRow = 64;
        currentOrderIndex = ~0;
        advanceRow();
    }

    @Override
    public void rewind(int subsong) throws IOException {
        write(0, 1, 1 << 5);

        current0xBD = rhythmMode ? (1 << 5) : 0;
        write(0, 0xbd, current0xBD);

        Arrays.fill(current0xBx, 0);
        Arrays.fill(channelCurrentInstrument, null);

        Instrument defaultInstrument = new Instrument();
        defaultInstrument.op[0].ksl = 0;
        defaultInstrument.op[0].multiple = 1;
        defaultInstrument.op[0].attackRate = 15;
        defaultInstrument.op[0].decayRate = 5;
        defaultInstrument.op[0].sustainLevel = 0;
        defaultInstrument.op[0].releaseRate = 1;
        defaultInstrument.op[0].waveSelect = 0;
        
        defaultInstrument.op[1].ksl = 0;
        defaultInstrument.op[1].multiple = 1;
        defaultInstrument.op[1].attackRate = 15;
        defaultInstrument.op[1].decayRate = 7;
        defaultInstrument.op[1].sustainLevel = 0;
        defaultInstrument.op[1].releaseRate = 2;
        defaultInstrument.op[1].waveSelect = 0;

        defaultInstrument.feedback = 3;
        defaultInstrument.connection = 0;
        defaultInstrument.name = "Default";

        for (int i = 0; i < 11; i++) {
            setInstrument(i, defaultInstrument);
        }

        songDone = false;
        resetPlayer();
    }

    @Override
    public float getRefresh() {
        return speedRowsPerSec;
    }


}
