/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;

import vavi.sound.SoundUtil;


/**
 * データを演奏する操作を提供する抽象クラス。
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.2 $
 */
public abstract class SoundPlayer {

    protected AudioFormat format;

    /**
     * サンプリングレートをfloatで返す。
     *
     * @return サンプリングレート
     */
    public float getSampleRate() {
        return format.getSampleRate();
    }

    /**
     * フレームサイズを返す。
     *
     * @return フレームサイズ
     */
    public int getFrameSize() {
        return format.getFrameSize();
    }

    /**
     * ラインを返す。
     *
     * @return ライン
     */
    public abstract DataLine getLine();

    /**
     * 再生を開始する。
     */
    public void start() {
        getLine().start();
    }

    /**
     * バッファにたまっているデータを掃き出す。
     */
    public void drain() {
        getLine().drain();
    }

    /**
     * 再生を終了する。
     */
    public void stop() {
        getLine().stop();
    }

    /**
     * ラインを閉じる。
     */
    public void close() {
        getLine().close();
    }

    /** change volume */
    public void volume(float gain) {
        SoundUtil.volume(getLine(), gain);
    }
}
