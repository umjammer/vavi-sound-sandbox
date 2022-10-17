/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.File;
import java.util.regex.Pattern;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiSystem;
import vavi.sound.mfi.Sequence;
import vavi.sound.mfi.Sequencer;
import vavi.util.Debug;

import vavix.util.grep.FileDigger;
import vavix.util.grep.RegexFileDigger;


/**
 * Analyze MLD.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030714 nsano initial version <br>
 */
public class t150_4 {

    /**
     * usage: java t150_4 dir
     */
    public static void main(String[] args) throws Exception {
        Sequencer sequencer = MfiSystem.getSequencer();
        sequencer.open();
        FileDigger fileDigger = new RegexFileDigger(file -> {
Debug.println("************ START ************* : " + file);
            try {
                Sequence sequence = MfiSystem.getSequence(file);
                sequencer.setSequence(sequence);
                sequencer.addMetaEventListener(meta -> {
Debug.println(meta.getType());
                    if (meta.getType() == 47) {
                    }
                });
                sequencer.start();
Debug.println("************ END *************");
            } catch (InvalidMfiDataException e) {
                e.printStackTrace();
            }
        }, Pattern.compile(".+\\.mld"));
        fileDigger.dig(new File(args[0]));
    }
}

/* */
