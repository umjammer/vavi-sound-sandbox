/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound.tone;

/**
 * ダミーエンベロープをあらわすクラス。
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1 $
 */
public class DummyEnvelope extends Envelope {

    public DummyEnvelope() {
    }

    @Override
    protected double getValueInPressing() {
        return 1.0;
    }

    @Override
    protected double getValueInReleasing() {
        return 1.0;
    }
}
