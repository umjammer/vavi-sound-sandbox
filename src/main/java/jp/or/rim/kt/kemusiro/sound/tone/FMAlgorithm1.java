/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound.tone;

/**
 * FM sound source algorithm 1.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1 $
 */
public class FMAlgorithm1 extends FMAlgorithm {

    public FMAlgorithm1(FMParameter p) {
        super(4);

        SawWave pitch = new SawWave();
        setInput(0, pitch, null);
        setInput(1, pitch, null);
        setInput(2, getOperator(0), getOperator(1));
        setInput(3, pitch, getOperator(2));
        setParameter(p);
    }

    @Override
    public double getValue(int number, double time) {
        return getOperator(3).getValue(number, time);
    }
}
