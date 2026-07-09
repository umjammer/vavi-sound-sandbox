/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.opl3;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

import vavi.sound.midi.opl3.Opl3Synthesizer.Context;


/** mark/reset are inside this method */
public abstract class MidiTypeFile {

    private static final Logger logger = System.getLogger(MidiTypeFile.class.getName());

    /** midi like file types */
    private static final List<MidiTypeFile> midiTypeFiles = ServiceLoader.load(MidiTypeFile.class).stream().map(Provider::get).toList();

    public static MidiTypeFile getFileType(String s) {
        return midiTypeFiles.stream().filter(f -> f.getClass().getName().contains(s)).findFirst().orElseThrow();
    }

    /**
     * is midi like file or not
     *
     * @throws NoSuchElementException when not found
     */
    public static MidiTypeFile getFileType(InputStream is) {
        return midiTypeFiles.stream().filter(f -> f.matchFormat(is)).findFirst().orElseThrow();
    }

    public static int maxMarkSize(InputStream is) {
        return midiTypeFiles.stream().mapToInt(MidiTypeFile::markSize).max().getAsInt();
    }

    boolean matchFormat(InputStream bitStream) {
        DataInputStream dis = new DataInputStream(bitStream);
        try {
            dis.mark(markSize());
            return matchFormatImpl(dis);
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return false;
        } finally {
            try {
                dis.reset();
            } catch (IOException e) {
                logger.log(Level.DEBUG, e.toString());
            }
        }
    }

    abstract int markSize();

    /** no need to mark/reset inside this method */
    abstract boolean matchFormatImpl(DataInputStream dis) throws IOException;

    abstract void rewind(int subSong, MidPlayer player) throws IOException;

    public abstract void init(Context context);

    public void controlChange(int channel, int controller, int value) {
    }

    public abstract String desc();
}
