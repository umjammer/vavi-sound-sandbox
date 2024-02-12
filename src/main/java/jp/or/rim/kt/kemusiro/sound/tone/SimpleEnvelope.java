/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound.tone;

/**
 * 単純エンベロープをあらわすクラス。
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1 $
 */
public class SimpleEnvelope extends Envelope {

    @Override
    protected double getValueInPressing() {
        return 1.0;
    }

    @Override
    protected double getValueInReleasing() {
        return 0.0;
    }
}
