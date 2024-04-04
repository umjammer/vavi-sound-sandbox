/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

/**
 * A class that represents an event that changes the tone.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.2 $
 */
public class ChangeInstrument extends MusicEvent {

    private final Instrument instrument;

    /**
     * Creates an event to change the tone.
     *
     * @param newTick       tick
     * @param newChannel    channel number
     * @param newInstrument new tone
     */
    public ChangeInstrument(int newTick, int newChannel, Instrument newInstrument) {
        tick = newTick;
        channel = newChannel;
        instrument = newInstrument;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public String toString() {
        return "Change Instrument " + instrument.toString();
    }
}
