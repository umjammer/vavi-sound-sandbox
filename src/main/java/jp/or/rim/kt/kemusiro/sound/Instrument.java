/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import jp.or.rim.kt.kemusiro.sound.tone.Envelope;
import jp.or.rim.kt.kemusiro.sound.tone.WaveGeneratable;


/**
 * 楽器を表すクラス。
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.2 $
 */
public abstract class Instrument {

    protected WaveGeneratable wave;
    protected Envelope envelope;
    private double currentTime;
    private double timeStep;

    public void setTimeStep(double newTimeStep) {
        timeStep = newTimeStep;
        envelope.setTimeStep(newTimeStep);
    }

    /**
     * 現在時刻の波形値を得る。波形の振幅は1.0に正規化される。
     * 値を取得した後、あらかじめ設定しておいた時間幅だけ、現在時刻を
     * 更新する。
     *
     * @param number 音番号(0-127)
     * @return 波形値
     */
    public double getValue(int number) {
        double value = envelope.getValue() * wave.getValue(number, currentTime);
        currentTime += timeStep;
        return value;
    }

    public void press() {
        currentTime = 0;
        envelope.press();
    }

    public void release() {
        envelope.release();
    }

    /**
     * 楽器の名前を得る。
     *
     * @return 楽器名
     */
    public abstract String getName();
}
