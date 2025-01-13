/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;

import vavi.sound.midi.MidiConstants.MetaEvent;


/**
 * A class that represents an event that changes the tempo.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.2 $
 */
public class ChangeTempo extends MusicEvent implements MidiConvertible {

    private final int tempo;

    public ChangeTempo(int tick, int channel, int tempo) {
        this.tick = tick;
        this.channel = channel;
        this.tempo = tempo;
    }

    public int getTempo() {
        return tempo;
    }

    @Override
    public String toString() {
        return "Change Tempo " + tempo;
    }

    @Override
    public MidiEvent[] convert(MidiContext context) throws InvalidMidiDataException {
        int tempo = context.getMidiTempo(this.tempo);
        MetaMessage metaMessage = new MetaMessage();
        metaMessage.setMessage(
                MetaEvent.META_TEMPO.number(),
                new byte[] {
                        (byte)  ((tempo / 0x10000) & 0xff),
                        (byte) (((tempo % 0x10000) / 0x100) & 0xff),
                        (byte)  ((tempo % 0x100)   & 0xff)
                },
                3);
        return new MidiEvent[] {
                new MidiEvent(metaMessage, tick)
        };
    }
}
