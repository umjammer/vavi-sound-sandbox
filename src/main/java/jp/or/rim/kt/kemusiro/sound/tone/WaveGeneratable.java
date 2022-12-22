/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound.tone;

/**
 * 音声を発生しうるインターフェース。
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1 $
 */
public interface WaveGeneratable {

    /**
     * 指定の時刻の値を得る。波形の振幅は1.0に正規化される。
     *
     * @param number 音番号(0-127)
     * @param time   時刻
     * @return 波形値
     */
    double getValue(int number, double time);
}
