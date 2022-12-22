/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

/**
 * イベントを表すクラス。
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1.1.1 $
 */
public abstract class MusicEvent implements Comparable<MusicEvent> {

    protected int tick;
    protected int channel;

    public int compareTo(MusicEvent o) {
        if (o != null) {
            return Integer.compare(this.tick, o.tick);
        } else {
            throw new ClassCastException();
        }
    }

    /**
     * ティック数を得る。
     *
     * @return ティック数
     */
    public int getTick() {
        return tick;
    }

    /**
     * チャンネル番号を得る。
     *
     * @return チャネル番号
     */
    public int getChannel() {
        return channel;
    }
}
