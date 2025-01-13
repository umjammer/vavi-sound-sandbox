/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;


/**
 * A class that represents a note off event.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.2 $
 */
public class NoteOff extends MusicEvent implements MidiConvertible {

    /** 0 - 127 */
    private final int number;
    /** 0 - 127 (0 means note-off) */
    private final int velocity;

    public NoteOff(int tick, int channel, int number, int velocity) {
        this.tick = tick;
        this.channel = channel;
        this.number = number;
        this.velocity = velocity;
    }

    @Override
    public String toString() {
        return "Note OFF " + number;
    }

    @Override
    public MidiEvent[] convert(MidiContext context) throws InvalidMidiDataException {
        ShortMessage shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.NOTE_OFF,
                channel,
                number,
                0);
        return new MidiEvent[] {
                new MidiEvent(shortMessage, tick)
        };
    }
}
