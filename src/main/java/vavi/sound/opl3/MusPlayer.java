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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;
import vavi.sound.SoundUtil;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

import static java.lang.System.getLogger;

/**
 * AdLib MIDI Music File (MUS) / IMPlay Song (IMS) Player.
 * Ported from adplug's mus.cpp / mus.h by Stas'M, based on PLAY.C by
 * Marc Savary, Ad Lib Inc.
 *
 * Instrument timbres live in companion files: SND banks (.snd/.tim/.tbr,
 * "timbres.snd") for MUS and BNK banks (.bnk, "implay.bnk", "standard.bnk")
 * for IMS. They are resolved relative to the source file when its URI is
 * known (props key "uri"); without a bank the default piano instrument is
 * used on all voices.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class MusPlayer extends ComposerPlayer {

    private static final Logger logger = getLogger(MusPlayer.class.getName());

    private static final int SYSTEM_XOR_BYTE = 0xF0;
    private static final int EOX_BYTE = 0xF7;
    private static final int OVERFLOW_BYTE = 0xF8;
    private static final int STOP_BYTE = 0xFC;

    private static final int NOTE_OFF_BYTE = 0x80;
    private static final int NOTE_ON_BYTE = 0x90;
    private static final int AFTER_TOUCH_BYTE = 0xA0;
    private static final int CONTROL_CHANGE_BYTE = 0xB0;
    private static final int PROG_CHANGE_BYTE = 0xC0;
    private static final int CHANNEL_PRESSURE_BYTE = 0xD0;
    private static final int PITCH_BEND_BYTE = 0xE0;

    /** for System exclusive */
    private static final int ADLIB_CTRL_BYTE = 0x7F;
    private static final int TEMPO_CTRL_BYTE = 0;

    private static final int OVERFLOW_TICKS = 240;
    private static final float MAX_SEC_DELAY = 10.0f;
    private static final int HEADER_LEN = 70;
    private static final int SND_HEADER_LEN = 6;
    private static final int IMS_SIGNATURE = 0x7777;
    /** ADLIB_INST_LEN 16-bit words */
    private static final int TIMBRE_DEF_SIZE = ADLIB_INST_LEN * 2;

    private static final String[] KNOWN_MUS_EXT = { "mus", "mdy", "ims" };
    private static final String[] KNOWN_SND_EXT = { "snd", "tim", "tbr" };
    private static final String[] KNOWN_SND_NAME = { "", "timbres" };
    private static final String[] KNOWN_BNK_NAME = { "", "implay", "standard" };

    private static class MusInst {
        String name = "";
        int backendIndex = -1;
    }

    // playback state
    private int pos;
    private boolean songend;
    private float timer;

    /** tick counter */
    private long counter;
    /** ticks to wait for next event */
    private long ticks;
    /** running status byte */
    private int status;
    /** actual volume of all voices */
    private final int[] volume = new int[MAX_VOICES];

    // header variables of .MUS file
    private int majorVersion;
    private int minorVersion;
    private String tuneName = "";
    private int tickBeat;
    private int dataSize;
    /** 0: melodic, 1: percussive */
    private int soundMode;
    /** 1 - 12 */
    private int pitchBRange;
    private int basicTempo;

    /** MIDI data */
    private byte[] data;
    /** play as IMS format */
    private boolean isIMS;

    /** instrument definitions */
    private MusInst[] insts;
    private int nrTimbre;

    @Override
    public Type getType() {
        return new Opl3FileFormatType("AdLib Visual Composer: MIDI Format", "mus");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("MUS");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            // MUS has no signature; adplug validates by file extension plus
            // header sanity. Gate on the extension when the URI is known.
            URI uri = SoundUtil.getSource(bitStream);
            if (uri != null) {
                String path = uri.getPath();
                if (path != null) {
                    boolean known = false;
                    String lower = path.toLowerCase();
                    for (String ext : KNOWN_MUS_EXT) {
                        if (lower.endsWith("." + ext)) {
                            known = true;
                            break;
                        }
                    }
                    if (!known) return false;
                }
            }
            bitStream.mark(HEADER_LEN);
            byte[] hdr = new byte[HEADER_LEN];
            if (bitStream.read(hdr) < HEADER_LEN) return false;
            return validateHeader(hdr);
        } catch (Exception e) {
            return false;
        } finally {
            try {
                bitStream.reset();
            } catch (IOException ignored) {}
        }
    }

    private static boolean validateHeader(byte[] hdr) {
        int major = hdr[0] & 0xff;
        int minor = hdr[1] & 0xff;
        long tuneId = u32(hdr, 2);
        int tickBeat = hdr[36] & 0xff;
        int beatMeasure = hdr[37] & 0xff;
        long totalTick = u32(hdr, 38);
        long dataSize = u32(hdr, 42);
        long nrCommand = u32(hdr, 46);
        return major == 1 && minor == 0 && tuneId == 0 &&
                tickBeat != 0 && beatMeasure != 0 && totalTick != 0 &&
                dataSize != 0 && nrCommand != 0;
    }

    @Override
    public int getTotalMilliseconds() {
        return 0;
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();
        if (buf.length < HEADER_LEN || !validateHeader(buf)) {
            throw new IllegalArgumentException("invalid header");
        }

        // read file header
        isIMS = false;
        insts = null;
        majorVersion = buf[0] & 0xff;
        minorVersion = buf[1] & 0xff;
        tuneName = readString(buf, 6, 30);
        tickBeat = buf[36] & 0xff;
        dataSize = (int) u32(buf, 42);
        // filler(8) at 50
        soundMode = buf[58] & 0xff;
        pitchBRange = buf[59] & 0xff;
        basicTempo = u16(buf, 60);
        // filler(8) at 62

        if (buf.length < HEADER_LEN + dataSize) {
            throw new IllegalArgumentException("truncated MIDI data");
        }

        // read MIDI data
        data = new byte[dataSize];
        System.arraycopy(buf, HEADER_LEN, data, 0, dataSize);

        // read IMS timbre list (if exists)
        if (buf.length >= HEADER_LEN + dataSize + 4 &&
                u16(buf, HEADER_LEN + dataSize) == IMS_SIGNATURE) {
            isIMS = true;
            nrTimbre = u16(buf, HEADER_LEN + dataSize + 2);
            // validate post-data size
            if (buf.length >= HEADER_LEN + dataSize + 4 + nrTimbre * INS_MAX_NAME_SIZE) {
                insts = new MusInst[nrTimbre];
                // read timbre names
                for (int i = 0; i < nrTimbre; i++) {
                    insts[i] = new MusInst();
                    insts[i].name = readString(buf, HEADER_LEN + dataSize + 4 + i * INS_MAX_NAME_SIZE, INS_MAX_NAME_SIZE);
                }
            } else {
                nrTimbre = 0;
            }
        }

logger.log(Level.DEBUG, "tune: " + tuneName.trim() + ", isIMS: " + isIMS + ", soundMode: " + soundMode + ", tempo: " + basicTempo);

        // load timbre banks from companion files, if we know where we are
        Path source = getSourcePath();
        if (source != null) {
            if (insts == null) {
                // load SND timbre bank
                bankSearch:
                for (String nam : KNOWN_SND_NAME) {
                    for (String ext : KNOWN_SND_EXT) {
                        for (Path fn : candidates(source, nam, ext)) {
                            if (loadTimbreBank(fn)) {
                                break bankSearch;
                            }
                        }
                    }
                }
            } else {
                // fetch timbre data from BNK banks
                for (String nam : KNOWN_BNK_NAME) {
                    if (instsLoaded()) {
                        break;
                    }
                    for (Path fn : candidates(source, nam, "bnk")) {
                        if (fetchTimbreData(fn)) {
                            break;
                        }
                    }
                }
            }
        } else {
logger.log(Level.DEBUG, "no source uri; timbre banks unavailable, using default instruments");
        }

        rewind(0);
    }

    /** resolves the source file path from props (key "uri"), null if unavailable */
    private Path getSourcePath() {
        try {
            Object o = props.get("uri");
            if (o == null) {
                return null;
            }
            URI uri = o instanceof URI ? (URI) o : URI.create(o.toString());
            return Paths.get(uri);
        } catch (Exception e) {
logger.log(Level.DEBUG, e.toString());
            return null;
        }
    }

    /** candidate bank paths: same basename (empty name) or fixed name in the same dir, plus the uppercase variant */
    private static Path[] candidates(Path source, String name, String ext) {
        String fileName = source.getFileName().toString();
        Path dir = source.getParent();
        if (name.isEmpty()) {
            // replace the (3-char) extension
            String base = fileName.length() > 3 ? fileName.substring(0, fileName.length() - 3) : fileName;
            return new Path[] {
                source.resolveSibling(base + ext),
                source.resolveSibling(base + ext.toUpperCase())
            };
        } else {
            Path d = dir != null ? dir : Paths.get(".");
            return new Path[] {
                d.resolve(name + "." + ext),
                d.resolve(name.toUpperCase() + "." + ext.toUpperCase())
            };
        }
    }

    private boolean instsLoaded() {
        if (insts == null) return false;
        for (int i = 0; i < nrTimbre; i++) {
            if (insts[i].backendIndex < 0) {
                return false;
            }
        }
        return true;
    }

    /** loads an SND timbre bank (whole-bank format: names then 16-bit-word timbre data) */
    private boolean loadTimbreBank(Path fn) {
        if (!Files.isReadable(fn)) {
            return false;
        }
        try {
            byte[] bank = Files.readAllBytes(fn);
            if (bank.length < SND_HEADER_LEN) {
logger.log(Level.DEBUG, "Timbre bank size is wrong.");
                return false;
            }
            int vMaj = bank[0] & 0xff;
            int vMin = bank[1] & 0xff;
            int nr = u16(bank, 2);
            int offsetDef = u16(bank, 4);
            // validate header and data size
            if (vMaj != 1 || vMin != 0 ||
                    offsetDef != SND_HEADER_LEN + nr * INS_MAX_NAME_SIZE ||
                    bank.length < SND_HEADER_LEN + nr * INS_MAX_NAME_SIZE + nr * TIMBRE_DEF_SIZE) {
logger.log(Level.DEBUG, "Timbre bank format is incorrect: " + fn);
                return false;
            }
            nrTimbre = nr;
            insts = new MusInst[nrTimbre];
            // read timbre names
            for (int i = 0; i < nrTimbre; i++) {
                insts[i] = new MusInst();
                insts[i].name = readString(bank, SND_HEADER_LEN + i * INS_MAX_NAME_SIZE, INS_MAX_NAME_SIZE);
            }
            // read timbre data (16-bit words, low byte significant)
            int off = offsetDef;
            for (int i = 0; i < nrTimbre; i++) {
                byte[] insData = new byte[ADLIB_INST_LEN];
                for (int j = 0; j < ADLIB_INST_LEN; j++) {
                    insData[j] = (byte) (u16(bank, off) & 0xff);
                    off += 2;
                }
                insts[i].backendIndex = load_instrument_data(insData, insData.length);
            }
logger.log(Level.DEBUG, "loaded SND timbre bank: " + fn + ", timbres: " + nrTimbre);
            return true;
        } catch (IOException e) {
logger.log(Level.DEBUG, e.toString());
            return false;
        }
    }

    /** fetches timbres by name from a BNK instrument bank for the IMS timbre list */
    private boolean fetchTimbreData(Path fn) {
        if (!Files.isReadable(fn)) {
            return false;
        }
        try {
            byte[] bank = Files.readAllBytes(fn);
            // BNK header: verMajor(1) verMinor(1) signature(6) numUsed(2) numEntries(2) offsetNames(4) offsetData(4)
            if (bank.length < 16) {
                return false;
            }
            int numEntries = u16(bank, 10);
            long offsetNames = u32(bank, 12);
            long offsetData = u32(bank, 16);

            for (int i = 0; i < nrTimbre; i++) {
                if (insts[i].backendIndex >= 0) {
                    continue;
                }
                // linear search of the name list: index(2) used(1) name(INS_MAX_NAME_SIZE)
                for (int e = 0; e < numEntries; e++) {
                    int entryOff = (int) offsetNames + e * (3 + INS_MAX_NAME_SIZE);
                    if (entryOff + 3 + INS_MAX_NAME_SIZE > bank.length) {
                        break;
                    }
                    int index = u16(bank, entryOff);
                    int used = bank[entryOff + 2] & 0xff;
                    if (used == 0) {
                        continue;
                    }
                    String name = readString(bank, entryOff + 3, INS_MAX_NAME_SIZE);
                    if (!name.equalsIgnoreCase(insts[i].name)) {
                        continue;
                    }
                    // record: mode(1) voiceNumber(1) + 28 bytes of raw instrument data
                    int recOff = (int) offsetData + index * (ADLIB_INST_LEN + 2);
                    if (recOff + 2 + ADLIB_INST_LEN > bank.length) {
                        break;
                    }
                    byte[] insData = new byte[ADLIB_INST_LEN];
                    System.arraycopy(bank, recOff + 2, insData, 0, ADLIB_INST_LEN);
                    insts[i].backendIndex = load_instrument_data(insData, insData.length);
                    break;
                }
            }
logger.log(Level.DEBUG, "fetched BNK timbre data: " + fn);
            return true;
        } catch (IOException e) {
logger.log(Level.DEBUG, e.toString());
            return false;
        }
    }

    @Override
    protected void frontend_rewind(int subsong) {
        setTempo(basicTempo, tickBeat);
        pos = 0;
        songend = false;

        SetRhythmMode(soundMode);
        SetPitchRange(pitchBRange);

        for (int i = 0; i < MAX_VOICES; i++) {
            volume[i] = 0;
            SetDefaultInstrument(i);
        }
        counter = 0;
        ticks = 0;
    }

    /** changes the tempo */
    private void setTempo(int tempo, int tickBeat) {
        if (tempo == 0) tempo = basicTempo;
        timer = tempo * tickBeat / 60.0f;
    }

    private long getTicks() {
        long t = 0;
        while (pos < dataSize && (data[pos] & 0xff) == OVERFLOW_BYTE) {
            t += OVERFLOW_TICKS;
            pos++;
        }
        if (pos < dataSize) {
            t += data[pos++] & 0xff;
        }
        // this check reduces delay and makes loops smoother
        if (t / timer > MAX_SEC_DELAY) { // for very long delays
            t = (long) (timer * MAX_SEC_DELAY);
        }
        return t;
    }

    private int d(int p) {
        return p < dataSize ? data[p] & 0xff : 0;
    }

    private void executeCommand() {
        int newStatus;

        // execute MIDI command
        if (d(pos) < NOTE_OFF_BYTE) {
            // running status
            newStatus = status;
        } else {
            newStatus = d(pos); pos++;
        }
        if (newStatus == STOP_BYTE) {
            pos = dataSize;
        } else if (newStatus == SYSTEM_XOR_BYTE) {
            // non-standard... this is a tempo multiplier:
            // data format: <F0> <7F> <00> <integer> <frac> <F7>
            // tempo = basicTempo * integerPart + basicTempo * fractionPart/128
            int b1 = d(pos); pos++;
            int b2 = d(pos); pos++;
            if (b1 != ADLIB_CTRL_BYTE || b2 != TEMPO_CTRL_BYTE) {
                // unknown format ... skip all the XOR message
                pos -= 2;
                while (pos < dataSize && d(pos++) != EOX_BYTE);
            } else {
                int integer = d(pos); pos++;
                int frac = d(pos); pos++;
                int tempo = basicTempo * integer + ((basicTempo * frac) >> 7);
                setTempo(tempo, tickBeat);
                pos++; // skip EOX_BYTE
            }
        } else {
            status = newStatus;
            int voice = status & 0xF;
            int haut, vol, timbre, pitch;
            switch (status & 0xF0) {
            case NOTE_ON_BYTE:
                haut = d(pos); pos++;
                vol = d(pos); pos++;
                if (voice >= MAX_VOICES) {
                    break;
                }
                if (vol == 0) {
                    NoteOff(voice);
                } else {
                    if (vol != volume[voice]) {
                        SetVolume(voice, vol);
                        volume[voice] = vol;
                    }
                    NoteOn(voice, haut);
                }
                break;
            case NOTE_OFF_BYTE:
                haut = d(pos); pos++;
                vol = d(pos); pos++;
                if (voice >= MAX_VOICES) {
                    break;
                }
                NoteOff(voice);
                if (isIMS && vol != 0) {
                    if (vol != volume[voice]) {
                        SetVolume(voice, vol);
                        volume[voice] = vol;
                    }
                    NoteOn(voice, haut);
                }
                break;
            case AFTER_TOUCH_BYTE:
                vol = d(pos); pos++;
                if (voice >= MAX_VOICES) {
                    break;
                }
                if (vol != volume[voice]) {
                    SetVolume(voice, vol);
                    volume[voice] = vol;
                }
                break;
            case PROG_CHANGE_BYTE:
                timbre = d(pos); pos++;
                if (voice >= MAX_VOICES) {
                    break;
                }
                if (insts != null) {
                    if (timbre < nrTimbre && insts[timbre].backendIndex >= 0) {
                        SetInstrument(voice, insts[timbre].backendIndex);
                    } else {
logger.log(Level.DEBUG, "Timbre not found: " + timbre);
                        SetDefaultInstrument(voice);
                    }
                }
                break;
            case PITCH_BEND_BYTE:
                pitch = d(pos); pos++;
                pitch |= d(pos) << 7; pos++;
                if (voice >= MAX_VOICES) {
                    break;
                }
                ChangePitch(voice, pitch);
                break;
            case CONTROL_CHANGE_BYTE:
                // unused
                pos += 2;
                break;
            case CHANNEL_PRESSURE_BYTE:
                // unused
                pos++;
                break;
            default:
                // a bad status byte (or unimplemented MIDI command) has been
                // encountered; skip bytes until next timing byte followed by
                // status byte
logger.log(Level.DEBUG, "Bad MIDI status byte: " + status);
                while (d(pos++) < NOTE_OFF_BYTE && pos < dataSize);
                if (pos >= dataSize) {
                    break;
                }
                if (d(pos) != OVERFLOW_BYTE) {
                    pos--;
                }
                break;
            }
        }
    }

    @Override
    public boolean update() throws IOException {
        if (counter == 0) {
            ticks = getTicks();
        }
        if (++counter >= ticks) {
            counter = 0;
            while (pos < dataSize) {
                executeCommand();
                if (pos >= dataSize) {
                    pos = 0;
                    songend = true;
                    break;
                } else if (data[pos] == 0) { // if next delay is zero
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
        return timer;
    }

    // ----

    /** NUL-terminated string */
    private static String readString(byte[] buf, int offset, int maxLength) {
        int len = 0;
        while (len < maxLength && offset + len < buf.length && buf[offset + len] != 0) {
            len++;
        }
        return new String(buf, offset, len, StandardCharsets.US_ASCII);
    }

    /** little-endian unsigned 16-bit */
    private static int u16(byte[] b, int off) {
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8);
    }

    /** little-endian unsigned 32-bit */
    private static long u32(byte[] b, int off) {
        return (b[off] & 0xffL) | ((b[off + 1] & 0xffL) << 8) | ((b[off + 2] & 0xffL) << 16) | ((b[off + 3] & 0xffL) << 24);
    }
}
