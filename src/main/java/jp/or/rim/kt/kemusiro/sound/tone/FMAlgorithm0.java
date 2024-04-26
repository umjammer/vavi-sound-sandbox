/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound.tone;

/**
 * FM sound source algorithm 0.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1 $
 */
public class FMAlgorithm0 extends FMAlgorithm {
    public FMAlgorithm0(FMParameter p) {
        super(4);

        SawWave pitch = new SawWave();
        setInput(0, pitch, null);
        setInput(1, pitch, getOperator(0));
        setInput(2, pitch, getOperator(1));
        setInput(3, pitch, getOperator(2));
        setParameter(p);
    }

    @Override
    public double getValue(int number, double time) {
        return getOperator(3).getValue(number, time);
    }
}
