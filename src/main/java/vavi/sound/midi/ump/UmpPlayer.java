/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.ump;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.concurrent.TimeUnit;

import static java.lang.System.getLogger;


/**
 * Plays a {@link MidiClipFile} in real time into a {@link UmpReceiver}, the MIDI 2.0 counterpart of
 * {@link javax.sound.midi.Sequencer}, only much less of a thing.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/09/11 nsano initial version <br>
 */
public class UmpPlayer {

    private static final Logger logger = getLogger(UmpPlayer.class.getName());

    /** 10 ns units per quarter note, that is 120 bpm, the tempo a clip starts at */
    public static final int DEFAULT_TEMPO = 50_000_000;

    private final UmpReceiver receiver;

    private volatile boolean playing;

    public UmpPlayer(UmpReceiver receiver) {
        this.receiver = receiver;
    }

    /**
     * Plays the whole clip, blocking until its End of Clip or until {@link #stop()}. Every note is
     * silenced on the way out, however the clip ended.
     */
    public void play(MidiClipFile clip) throws InterruptedException {
        int resolution = clip.getResolution();
        long tempo = DEFAULT_TEMPO;
        long start = System.nanoTime();
        long elapsed = 0;
        long lastTick = 0;
        StringBuilder text = new StringBuilder();

        playing = true;
        try {
            for (MidiClipFile.Event event : clip.getEvents()) {
                int[] words = event.words();
                // the tempo of the moment applies to the wait, the one this message may bring does not
                elapsed += (event.tick() - lastTick) * nanosPerTick(resolution, tempo);
                lastTick = event.tick();
                long wait = start + elapsed - System.nanoTime();
                if (wait > 0) {
                    TimeUnit.NANOSECONDS.sleep(wait);
                }
                if (!playing) {
                    break;
                }

                if (Ump.isSetTempo(words)) {
                    tempo = Integer.toUnsignedLong(words[1]);
logger.log(Level.DEBUG, "tempo: %d bpm".formatted(60_000_000_000L / (tempo * 10)));
                } else if (Ump.isText(words)) {
                    logText(text, words);
                }

                receiver.send(words);

                if (Ump.messageType(words[0]) == Ump.STREAM && Ump.streamStatus(words[0]) == Ump.END_OF_CLIP) {
                    break;
                }
            }
        } finally {
            playing = false;
            allNotesOff();
        }
    }

    /** Asks a {@link #play(MidiClipFile)} in flight to give up, it returns once it wakes. */
    public void stop() {
        playing = false;
    }

    public boolean isPlaying() {
        return playing;
    }

    /** how long one Delta Clockstamp tick lasts */
    private static long nanosPerTick(int resolution, long tempo) {
        return resolution > 0 ? tempo * 10 / resolution :
                TimeUnit.SECONDS.toNanos(1) / MidiClipFile.DEFAULT_TICKS_PER_SECOND;
    }

    /** collects a metadata text, which is split over as many messages as its 12 bytes a message need */
    private static void logText(StringBuilder text, int[] words) {
        int form = Ump.flexForm(words[0]);
        if (form == 0 || form == 1) {
            text.setLength(0);
        }
        text.append(Ump.text(words));
        if (form == 0 || form == 3) {
logger.log(Level.DEBUG, "text[%02x]: %s".formatted(Ump.flexStatus(words[0]), text.toString().strip()));
        }
    }

    /** so that a clip cut short does not leave a note droning */
    private void allNotesOff() {
        for (int group = 0; group < 16; group++) {
            for (int channel = 0; channel < 16; channel++) {
                receiver.send(new int[] {
                        (Ump.MIDI1_CHANNEL_VOICE << 28) | (group << 24) |
                                (Ump.CONTROL_CHANGE << 20) | (channel << 16) | (123 << 8)
                });
            }
        }
    }
}
