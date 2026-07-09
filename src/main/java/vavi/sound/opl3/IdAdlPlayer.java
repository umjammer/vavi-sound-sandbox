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
import java.util.List;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;

import vavi.io.LittleEndianDataInputStream;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

import static java.lang.System.getLogger;


/**
 * id Software Adlib Sound Effect Player.
 *
 * @author Ben McLean <mclean.ben@gmail.com>
 */
public class IdAdlPlayer extends Opl3Player {

    private static final Logger logger = getLogger(IdAdlPlayer.class.getName());

    public static class Adl {
        public static final float Hz = 1.0f / 140.0f;
        public final byte[] instrument = new byte[16];
        public final int octave;
        public final byte[] notes;
        public final int priority;

        public Adl(InputStream stream) throws IOException {
            LittleEndianDataInputStream dis = new LittleEndianDataInputStream(stream);
            long length = dis.readInt() & 0xffffffffL;
            priority = dis.readShort() & 0xffff;
            dis.readFully(instrument);
            octave = dis.readByte() & 0xff;
            notes = new byte[(int) length];
            dis.readFully(notes);
        }

        public int getBlock() {
            return (octave & 7) << 2;
        }

        public static final int KEY_FLAG = 0x20;
        public static final int NOTE_PORT = 0xA0;
        public static final int OCTAVE_PORT = 0xB0;
        public static final List<Integer> INSTRUMENT_PORTS = List.of(
            0x20, 0x23, 0x40, 0x43, 0x60, 0x63, 0x80, 0x83, 0xE0, 0xE3
        );
    }

    private Adl currentSound;
    private int currentNote;
    private boolean note;

    public void setNote(boolean val) {
        this.note = val;
        if (note) {
            write(0, Adl.OCTAVE_PORT, currentSound.getBlock() | Adl.KEY_FLAG);
        } else {
            write(0, Adl.OCTAVE_PORT, 0);
        }
    }

    public boolean isNote() {
        return note;
    }

    public IdAdlPlayer setInstrument() {
        for (int i = 0; i < Adl.INSTRUMENT_PORTS.size(); i++) {
            write(0, Adl.INSTRUMENT_PORTS.get(i), currentSound.instrument[i] & 0xff);
        }
        write(0, 0xC0, 0);
        return this;
    }

    public void setCurrentSound(Adl value) {
        if (currentSound == null || value == null || value.priority >= currentSound.priority) {
            currentNote = 0;
            setNote(false);
            if ((currentSound = value) != null) {
                setInstrument();
                setNote(true);
            }
        }
    }

    public Adl getCurrentSound() {
        return currentSound;
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("id Software Adlib Sound Effect Format", "idadl");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("IDADL");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            java.net.URI uri = vavi.sound.SoundUtil.getSource(bitStream);
            if (uri != null) {
                String path = uri.getPath();
                if (path != null && path.toLowerCase().endsWith(".idadl")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(bitStream);
        try {
            dis.mark(24);
            long length = dis.readInt() & 0xffffffffL;
            int priority = dis.readShort() & 0xffff;
            if (length <= 0 || length > 1024 * 1024) {
                return false;
            }
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                dis.reset();
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
        setCurrentSound(new Adl(is));
        rewind(0);
    }

    @Override
    public boolean update() {
        if (currentSound != null) {
            if ((currentSound.notes[currentNote] & 0xff) == 0) {
                setNote(false);
            } else {
                if (!isNote()) {
                    setNote(true);
                }
                write(0, Adl.NOTE_PORT, currentSound.notes[currentNote] & 0xff);
            }
            currentNote++;
            if (currentNote >= currentSound.notes.length) {
                currentSound = null;
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public void rewind(int subSong) {
        currentNote = 0;
        setNote(false);
        for (int i = 0; i < 256; ++i) {
            write(0, i, 0);
            write(1, i, 0);
        }
        write(0, 1, 32);
        if (currentSound != null) {
            setInstrument();
            setNote(true);
        }
    }

    @Override
    public float getRefresh() {
        return 140.0f;
    }
}
