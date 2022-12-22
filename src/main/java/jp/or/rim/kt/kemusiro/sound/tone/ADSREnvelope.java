/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound.tone;

/**
 * エンベロープをあらわすクラス。
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1 $
 */
public class ADSREnvelope extends Envelope {

    private double attackRate;
    private double decayRate;
    private double sustainRate;
    private double releaseRate;
    private double sustainLevel;
    private double maxLevel;
    private double level;

    public ADSREnvelope(double ar,
                        double dr,
                        double sr,
                        double rr,
                        double sl,
                        double ml) {
        attackRate = ar;
        decayRate = dr;
        sustainRate = sr;
        releaseRate = rr;
        sustainLevel = sl;
        maxLevel = ml;
    }

    public double getValueInPressing() {
        double attackingTime = maxLevel / attackRate;
        if (time < attackingTime) {
            return level = attackRate * time;
        }
        double decayingTime = attackingTime + sustainLevel / decayRate;
        if (time < attackingTime + decayingTime) {
            return level = maxLevel - decayRate * (time - attackingTime);
        }
        return level = Math.max(maxLevel - sustainLevel - sustainRate * (time - decayingTime),
                0.0);
    }

    public double getValueInReleasing() {
        return Math.max(level - time * releaseRate, 0.0);
    }
}
