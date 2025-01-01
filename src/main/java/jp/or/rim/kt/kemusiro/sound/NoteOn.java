/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;


/**
 * A class that represents a note on events.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.2 $
 */
public class NoteOn extends MusicEvent implements MidiConvertible {

    /** 0 - 127 */
    private final int number;
    /** 0 - 127 (0 means note-off) */
    private final int velocity;

    public NoteOn(int tick, int channel, int number, int velocity) {
        this.tick = tick;
        this.channel = channel;
        this.number = number;
        this.velocity = velocity;
//Debug.println("tick: " + tick + ", channel: " + channel + ", number: " + number + ", velocity: " + velocity);
    }

    public int getNumber() {
        return number;
    }

    public int getVelocity() {
        return velocity;
    }

    @Override
    public String toString() {
        return "Note ON " + number;
    }

    @Override
    public MidiEvent[] convert(MidiContext context) throws InvalidMidiDataException {
        ShortMessage noteOnMessage = new ShortMessage();
        noteOnMessage.setMessage(ShortMessage.NOTE_ON,
                channel,
                number,
                velocity);
        ShortMessage noteOffMessage = new ShortMessage();
        noteOffMessage.setMessage(ShortMessage.NOTE_OFF,
                channel,
                number,
                0);
//logger.log(Level.TRACE, "note: " + channel + ": " + pitch);
        return new MidiEvent[] {
                new MidiEvent(noteOnMessage, tick),
//                new MidiEvent(noteOffMessage, tick + 120)
        };
    }
}
