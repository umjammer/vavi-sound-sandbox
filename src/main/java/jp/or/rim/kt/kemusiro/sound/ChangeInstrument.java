/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

/**
 * 音色を変えるイベントを表すクラス。
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.2 $
 */
public class ChangeInstrument extends MusicEvent {

    private final Instrument instrument;

    /**
     * 音色を変更するイベントを作成する。
     *
     * @param newTick       ティック
     * @param newChannel    チャネル番号
     * @param newInstrument 新しい音色
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
