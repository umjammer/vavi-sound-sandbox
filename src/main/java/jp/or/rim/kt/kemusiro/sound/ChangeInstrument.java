/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import vavi.util.Debug;


/**
 * A class that represents an event that changes the tone.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.2 $
 */
public class ChangeInstrument extends MusicEvent implements MidiConvertible {

    private final Instrument instrument;

    /**
     * Creates an event to change the tone.
     *
     * @param tick       tick
     * @param channel    channel number
     * @param instrument new tone
     */
    public ChangeInstrument(int tick, int channel, Instrument instrument) {
        this.tick = tick;
        this.channel = channel;
        this.instrument = instrument;
Debug.println("tick: " + tick + ", channel: " + channel + ", instrument: " + instrument);
    }

    public Instrument getInstrument() {
        return instrument;
    }

    @Override
    public String toString() {
        return "Change Instrument " + instrument;
    }

    @Override
    public MidiEvent[] convert(MidiContext context) throws InvalidMidiDataException {
        ShortMessage shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.PROGRAM_CHANGE,
                channel % 16,
                context.getBank(instrument),
                context.getProgram(instrument));
        return new MidiEvent[] {
                new MidiEvent(shortMessage, tick)
        };
    }
}
