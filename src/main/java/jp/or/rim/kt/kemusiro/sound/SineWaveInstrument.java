/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import jp.or.rim.kt.kemusiro.sound.tone.Envelope;
import jp.or.rim.kt.kemusiro.sound.tone.SimpleEnvelope;
import jp.or.rim.kt.kemusiro.sound.tone.SineWave;


/**
 * sin波を表すクラス。
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1 $
 */
public class SineWaveInstrument extends Instrument {

    public SineWaveInstrument() {
        wave = new SineWave();
        envelope = new SimpleEnvelope();
    }

    public SineWaveInstrument(Envelope envelope) {
        wave = new SineWave();
        this.envelope = envelope;
    }

    public String getName() {
        return toString();
    }

    public String toString() {
        return "Sine Wave";
    }
}
