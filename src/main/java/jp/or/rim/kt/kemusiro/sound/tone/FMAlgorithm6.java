/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound.tone;

/**
 * FM sound source algorithm 6.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1 $
 */
public class FMAlgorithm6 extends FMAlgorithm {

    public FMAlgorithm6(FMParameter p) {
        super(4);

        SawWave pitch = new SawWave();
        setInput(0, pitch, null);
        setInput(1, pitch, getOperator(0));
        setInput(2, pitch, null);
        setInput(3, pitch, null);
        setParameter(p);
    }

    @Override
    public double getValue(int number, double time) {
        return (getOperator(1).getValue(number, time) +
                getOperator(2).getValue(number, time) +
                getOperator(3).getValue(number, time)) / 3.0;
    }
}
