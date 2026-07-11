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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;
import vavi.sound.SoundUtil;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

import static java.lang.System.getLogger;

/**
 * AdLib Visual Composer ROL Player.
 * Ported from adplug's rol.cpp / rol.h by OPLx.
 *
 * Instruments come from a companion "standard.bnk" instrument bank in the
 * same directory, resolved via the source URI (props key "uri"); a missing
 * bank or missing instrument yields a silent all-zero instrument (as in
 * adplug).
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class RolPlayer extends ComposerPlayer {

    private static final Logger logger = getLogger(RolPlayer.class.getName());

    private static final int kMaxTickBeat = 60;
    private static final float kDefaultUpdateTme = 18.2f;

    private static final int kNoteEnd = 1;
    private static final int kPitchEnd = 1 << 1;
    private static final int kInstrEnd = 1 << 2;
    private static final int kVolumeEnd = 1 << 3;

    private static class NoteEvent {
        int number;
        int duration;
    }

    private static class InstrumentEvent {
        int time;
        int insIndex;
    }

    private static class VolumeEvent {
        int time;
        float multiplier;
    }

    private static class PitchEvent {
        int time;
        float variation;
    }

    private static class VoiceData {
        final List<NoteEvent> noteEvents = new ArrayList<>();
        final List<InstrumentEvent> instrumentEvents = new ArrayList<>();
        final List<VolumeEvent> volumeEvents = new ArrayList<>();
        final List<PitchEvent> pitchEvents = new ArrayList<>();
        int eventStatus;
        int noteDuration;
        int currentNoteDuration;
        int currentNote;
        int nextInstrumentEvent;
        int nextVolumeEvent;
        int nextPitchEvent;
        boolean forceNote = true;

        void reset() {
            eventStatus = 0;
            noteDuration = 0;
            currentNoteDuration = 0;
            currentNote = 0;
            nextInstrumentEvent = 0;
            nextVolumeEvent = 0;
            nextPitchEvent = 0;
            forceNote = true;
        }
    }

    private static class TempoEvent {
        int time;
        float multiplier;
    }

    // header fields
    private int ticksPerBeat;
    private int mode;
    private float basicTempo;

    private final List<TempoEvent> tempoEvents = new ArrayList<>();
    private final List<VoiceData> voiceData = new ArrayList<>();
    private float refresh = kDefaultUpdateTme;
    private int nextTempoEvent;
    private int currTick;
    private int timeOfLastNote;

    // BNK bank state
    private byte[] bank;
    private final Map<String, Integer> bankIndexCache = new HashMap<>();

    @Override
    public Type getType() {
        return new Opl3FileFormatType("AdLib Visual Composer", "rol");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("ROL");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            // the ROL "magic" is just version 0.4 - require the extension too
            // when the source URI is known
            URI uri = SoundUtil.getSource(bitStream);
            if (uri != null) {
                String path = uri.getPath();
                if (path != null && !path.toLowerCase().endsWith(".rol")) {
                    return false;
                }
            }
            bitStream.mark(4);
            byte[] hdr = new byte[4];
            if (bitStream.read(hdr) < 4) return false;
            int major = (hdr[0] & 0xff) | ((hdr[1] & 0xff) << 8);
            int minor = (hdr[2] & 0xff) | ((hdr[3] & 0xff) << 8);
            return major == 0 && minor == 4;
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
        // header: version(4) + comment(40) + ticks/beat(2) + beats/measure(2) +
        // scaleY(2) + scaleX(2) + unused(1) + mode(1) + unused2(90) + filler0(38) + filler1(15) + tempo(4)
        if (buf.length < 201) {
            throw new IllegalArgumentException("file too short");
        }
        int major = u16(buf, 0);
        int minor = u16(buf, 2);
        if (major != 0 || minor != 4) {
            throw new IllegalArgumentException("unsupported file version " + major + "." + minor + " or not a ROL file");
        }

        int offset = 4;
        offset += 40; // comment
        ticksPerBeat = u16(buf, offset); offset += 2;
        offset += 2; // beats_per_measure
        offset += 2; // edit_scale_y
        offset += 2; // edit_scale_x
        offset += 1; // unused1
        mode = buf[offset++] & 0xff;
        offset += 90 + 38 + 15; // unused2, filler0, filler1
        basicTempo = f32(buf, offset); offset += 4;

        // load the standard.bnk instrument bank from the song's directory
        bank = null;
        bankIndexCache.clear();
        Path source = getSourcePath();
        if (source != null) {
            Path dir = source.getParent();
            Path bnk = (dir != null ? dir : Paths.get(".")).resolve("standard.bnk");
            if (!Files.isReadable(bnk)) {
                bnk = (dir != null ? dir : Paths.get(".")).resolve("STANDARD.BNK");
            }
            if (Files.isReadable(bnk)) {
                bank = Files.readAllBytes(bnk);
logger.log(Level.DEBUG, "bnk: " + bnk);
            }
        }
        if (bank == null) {
logger.log(Level.DEBUG, "standard.bnk not found; instruments will be silent");
        }

        // tempo events
        tempoEvents.clear();
        int numTempoEvents = u16(buf, offset); offset += 2;
        for (int i = 0; i < numTempoEvents; i++) {
            TempoEvent event = new TempoEvent();
            event.time = s16(buf, offset); offset += 2;
            event.multiplier = f32(buf, offset); offset += 4;
            tempoEvents.add(event);
        }

        // voice data
        timeOfLastNote = 0;
        voiceData.clear();
        int numVoices = mode != 0 ? kNumMelodicVoices : kNumPercussiveVoices;
        for (int i = 0; i < numVoices; i++) {
            VoiceData voice = new VoiceData();
            offset = loadNoteEvents(buf, offset, voice);
            offset = loadInstrumentEvents(buf, offset, voice);
            offset = loadVolumeEvents(buf, offset, voice);
            offset = loadPitchEvents(buf, offset, voice);
            voiceData.add(voice);
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

    private int loadNoteEvents(byte[] buf, int offset, VoiceData voice) {
        offset += 15; // filler

        int timeOfLastNote = s16(buf, offset); offset += 2;

        if (timeOfLastNote != 0) {
            int totalDuration = 0;

            do {
                NoteEvent event = new NoteEvent();
                event.number = s16(buf, offset); offset += 2;
                event.duration = s16(buf, offset); offset += 2;

                voice.noteEvents.add(event);

                totalDuration += event.duration;
            } while (totalDuration < timeOfLastNote && offset + 4 <= buf.length);

            if (timeOfLastNote > this.timeOfLastNote) {
                this.timeOfLastNote = timeOfLastNote;
            }
        }

        offset += 15; // filler
        return offset;
    }

    private int loadInstrumentEvents(byte[] buf, int offset, VoiceData voice) {
        int count = u16(buf, offset); offset += 2;

        for (int i = 0; i < count; i++) {
            InstrumentEvent event = new InstrumentEvent();
            event.time = s16(buf, offset); offset += 2;
            String name = readString(buf, offset, INS_MAX_NAME_SIZE);
            offset += INS_MAX_NAME_SIZE;

            event.insIndex = loadBnkInstrument(name);

            voice.instrumentEvents.add(event);

            offset += 3; // 1 filler, 2 unused
        }

        offset += 15; // filler
        return offset;
    }

    private int loadVolumeEvents(byte[] buf, int offset, VoiceData voice) {
        int count = u16(buf, offset); offset += 2;

        for (int i = 0; i < count; i++) {
            VolumeEvent event = new VolumeEvent();
            event.time = s16(buf, offset); offset += 2;
            event.multiplier = f32(buf, offset); offset += 4;
            voice.volumeEvents.add(event);
        }

        offset += 15; // filler
        return offset;
    }

    private int loadPitchEvents(byte[] buf, int offset, VoiceData voice) {
        int count = u16(buf, offset); offset += 2;

        for (int i = 0; i < count; i++) {
            PitchEvent event = new PitchEvent();
            event.time = s16(buf, offset); offset += 2;
            event.variation = f32(buf, offset); offset += 4;
            voice.pitchEvents.add(event);
        }
        return offset;
    }

    /**
     * Looks up an instrument by name in the BNK bank and registers it with
     * the composer backend; missing instruments become all-zero (silent)
     * entries, as in adplug's load_bnk_instrument with bnk_return_failure
     * unset.
     */
    private int loadBnkInstrument(String name) {
        Integer cached = bankIndexCache.get(name);
        if (cached != null) {
            return cached;
        }

        byte[] insData = new byte[ADLIB_INST_LEN]; // default: all-zero (silent)

        if (bank != null && bank.length >= 20) {
            // BNK header: verMajor(1) verMinor(1) signature(6) numUsed(2) numEntries(2) offsetNames(4) offsetData(4)
            int numEntries = u16(bank, 10);
            long offsetNames = u32(bank, 12);
            long offsetData = u32(bank, 16);

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
                String entryName = readString(bank, entryOff + 3, INS_MAX_NAME_SIZE);
                if (!entryName.equalsIgnoreCase(name)) {
                    continue;
                }
                // record: mode(1) voiceNumber(1) + 28 bytes of raw instrument data
                int recOff = (int) offsetData + index * (ADLIB_INST_LEN + 2);
                if (recOff + 2 + ADLIB_INST_LEN > bank.length) {
                    break;
                }
                System.arraycopy(bank, recOff + 2, insData, 0, ADLIB_INST_LEN);
                break;
            }
        }

        int index = load_instrument_data(insData, insData.length);
        bankIndexCache.put(name, index);
        return index;
    }

    @Override
    public boolean update() throws IOException {
        if (nextTempoEvent < tempoEvents.size() &&
                tempoEvents.get(nextTempoEvent).time == currTick) {
            setRefresh(tempoEvents.get(nextTempoEvent).multiplier);
            nextTempoEvent++;
        }

        for (int voice = 0; voice < voiceData.size(); voice++) {
            updateVoice(voice, voiceData.get(voice));
        }

        currTick++;

        return currTick <= timeOfLastNote;
    }

    @Override
    protected void frontend_rewind(int subsong) {
        for (VoiceData voice : voiceData) {
            voice.reset();
        }

        nextTempoEvent = 0;
        currTick = 0;

        SetRhythmMode(mode ^ 1);

        setRefresh(1.0f);
    }

    private void setRefresh(float multiplier) {
        float tickBeat = Math.min(kMaxTickBeat, ticksPerBeat);

        refresh = (tickBeat * basicTempo * multiplier) / 60.0f;
    }

    @Override
    public float getRefresh() {
        return refresh;
    }

    private void updateVoice(int voice, VoiceData voiceData) {
        List<NoteEvent> nEvents = voiceData.noteEvents;

        if (nEvents.isEmpty() || (voiceData.eventStatus & kNoteEnd) != 0) {
            return; // no note data to process, don't bother doing anything
        }

        if ((voiceData.eventStatus & kInstrEnd) == 0) {
            if (voiceData.nextInstrumentEvent < voiceData.instrumentEvents.size()) {
                InstrumentEvent event = voiceData.instrumentEvents.get(voiceData.nextInstrumentEvent);
                if (event.time == currTick) {
                    SetInstrument(voice, event.insIndex);
                    voiceData.nextInstrumentEvent++;
                }
            } else {
                voiceData.eventStatus |= kInstrEnd;
            }
        }

        if ((voiceData.eventStatus & kVolumeEnd) == 0) {
            if (voiceData.nextVolumeEvent < voiceData.volumeEvents.size()) {
                VolumeEvent event = voiceData.volumeEvents.get(voiceData.nextVolumeEvent);
                if (event.time == currTick) {
                    int volume = (int) (kMaxVolume * event.multiplier) & 0xff;

                    SetVolume(voice, volume);

                    voiceData.nextVolumeEvent++; // move to next volume event
                }
            } else {
                voiceData.eventStatus |= kVolumeEnd;
            }
        }

        if (voiceData.forceNote || voiceData.currentNoteDuration > voiceData.noteDuration - 1) {
            if (currTick != 0) {
                voiceData.currentNote++;
            }

            if (voiceData.currentNote < nEvents.size()) {
                NoteEvent noteEvent = nEvents.get(voiceData.currentNote);

                NoteOn(voice, noteEvent.number);
                voiceData.currentNoteDuration = 0;
                voiceData.noteDuration = noteEvent.duration;
                voiceData.forceNote = false;
            } else {
                NoteOff(voice);
                voiceData.eventStatus |= kNoteEnd;
                return;
            }
        }

        if ((voiceData.eventStatus & kPitchEnd) == 0) {
            if (voiceData.nextPitchEvent < voiceData.pitchEvents.size()) {
                PitchEvent event = voiceData.pitchEvents.get(voiceData.nextPitchEvent);
                if (event.time == currTick) {
                    setPitch(voice, event.variation);
                    voiceData.nextPitchEvent++;
                }
            } else {
                voiceData.eventStatus |= kPitchEnd;
            }
        }

        voiceData.currentNoteDuration++;
    }

    private void setPitch(int voice, float variation) {
        int pitchBend = (variation == 1.0f) ? kMidPitch : ((int) ((0x3fff >> 1) * variation)) & 0xffff;

        ChangePitch(voice, pitchBend);
    }

    // ---- byte helpers

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

    /** little-endian signed 16-bit */
    private static int s16(byte[] b, int off) {
        return (short) u16(b, off);
    }

    /** little-endian unsigned 32-bit */
    private static long u32(byte[] b, int off) {
        return (b[off] & 0xffL) | ((b[off + 1] & 0xffL) << 8) | ((b[off + 2] & 0xffL) << 16) | ((b[off + 3] & 0xffL) << 24);
    }

    /** little-endian IEEE 754 single-precision float */
    private static float f32(byte[] b, int off) {
        return Float.intBitsToFloat((int) u32(b, off));
    }
}
