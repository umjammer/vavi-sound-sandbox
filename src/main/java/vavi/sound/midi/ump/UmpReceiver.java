/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.ump;


/**
 * Something that Universal MIDI Packets can be played on, the MIDI 2.0 counterpart of
 * {@link javax.sound.midi.Receiver}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/09/11 nsano initial version <br>
 */
@FunctionalInterface
public interface UmpReceiver {

    /**
     * Plays one message right now. A receiver is expected to quietly drop what it has no use for,
     * a Flex Data lyric on a synthesizer for instance.
     *
     * @param words one complete UMP message, 1 to 4 words
     */
    void send(int[] words);
}
