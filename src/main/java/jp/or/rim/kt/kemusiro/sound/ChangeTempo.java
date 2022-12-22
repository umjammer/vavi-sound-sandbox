/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

/**
 * テンポを変えるイベントを表すクラス。
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.2 $
 */
public class ChangeTempo extends MusicEvent {
    private int tempo;

    public ChangeTempo(int newTick, int newChannel, int newTempo) {
        tick = newTick;
        channel = newChannel;
        tempo = newTempo;
    }

    public int getTempo() {
        return tempo;
    }

    public String toString() {
        return "Change Tempo " + tempo;
    }
}
