/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.ittake;

import java.io.File;

import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;

import org.junit.jupiter.api.Test;

import vavi.sound.mfi.MfiSystem;
import vavi.sound.mfi.vavi.VaviMfiFileFormat;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.*;


/**
 * IttakeMidiConverterTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/04/08 umjammer initial version <br>
 */
class IttakeMidiConverterTest {

    @Test
    void test() {
        fail("Not yet implemented");
    }


    //-------------------------------------------------------------------------

    /**
     * Converts the midi file to a mfi file.
     * <pre>
     * usage:
     *  % java MfiContext in_midi_file out_mld_file
     * </pre>
     */
    public static void main(String[] args) throws Exception {

Debug.println("midi in: " + args[0]);
Debug.println("mfi out: " + args[1]);

        File file = new File(args[0]);
        javax.sound.midi.Sequence midiSequence = MidiSystem.getSequence(file);
        MidiFileFormat midiFileFormat = MidiSystem.getMidiFileFormat(file);
        int type = midiFileFormat.getType();
Debug.println("type: " + type);
        vavi.sound.mfi.Sequence mfiSequence = new IttakeMidiConverter().toMfiSequence(midiSequence, type);

        file = new File(args[1]);
        int r = MfiSystem.write(mfiSequence, VaviMfiFileFormat.FILE_TYPE, file);
Debug.println("write: " + r);

        System.exit(0);
    }
}

/* */
