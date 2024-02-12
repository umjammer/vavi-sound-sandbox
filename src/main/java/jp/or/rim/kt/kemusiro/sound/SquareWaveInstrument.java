/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import jp.or.rim.kt.kemusiro.sound.tone.Envelope;
import jp.or.rim.kt.kemusiro.sound.tone.SimpleEnvelope;
import jp.or.rim.kt.kemusiro.sound.tone.SquareWave;


/**
 * 方形波を表すクラス。
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1 $
 */
public class SquareWaveInstrument extends Instrument {

    public SquareWaveInstrument() {
        wave = new SquareWave();
        envelope = new SimpleEnvelope();
    }

    public SquareWaveInstrument(Envelope envelope) {
        wave = new SquareWave();
        this.envelope = envelope;
    }

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        return "Square Wave";
    }
}
