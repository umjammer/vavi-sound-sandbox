/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound.tone;

/**
 * A class that represents an envelope.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1 $
 */
public class ADSREnvelope extends Envelope {

    private final double attackRate;
    private final double decayRate;
    private final double sustainRate;
    private final double releaseRate;
    private final double sustainLevel;
    private final double maxLevel;
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

    @Override
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

    @Override
    public double getValueInReleasing() {
        return Math.max(level - time * releaseRate, 0.0);
    }
}
