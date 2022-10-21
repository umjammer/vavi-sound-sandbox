/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.File;

import vavi.sound.smaf.Sequence;
import vavi.sound.smaf.Sequencer;
import vavi.sound.smaf.SmafSystem;
import vavi.util.Debug;


/**
 * Play SMAF.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080421 nsano initial version <br>
 */
public class PlaySMAF {

    /**
     * usage: java PlaySMAF mmf ...
     */
    public static void main(String[] args) throws Exception {
        Sequencer sequencer = SmafSystem.getSequencer();
        sequencer.open();
        for (int i = 0; i < args.length; i++) {
Debug.println("START: " + args[i]);
            Sequence sequence = SmafSystem.getSequence(new File(args[i]));
            sequencer.setSequence(sequence);
            if (i == args.length - 1) {
                sequencer.addMetaEventListener(meta -> {
Debug.println(meta.getType());
                    if (meta.getType() == 47) {
                    }
                });
            }
            sequencer.start();
Debug.println("END: " + args[i]);
        }
    }
}

/* */
