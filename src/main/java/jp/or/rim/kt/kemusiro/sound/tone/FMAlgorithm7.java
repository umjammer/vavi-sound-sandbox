/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound.tone;

/**
 * FM sound source algorithm 7.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1 $
 */
public class FMAlgorithm7 extends FMAlgorithm {

    public FMAlgorithm7(FMParameter p) {
        super(4);

        SawWave pitch = new SawWave();
        setInput(0, pitch, null);
        setInput(1, pitch, null);
        setInput(2, pitch, null);
        setInput(3, pitch, null);
        setParameter(p);
    }

    @Override
    public double getValue(int number, double time) {
        return (getOperator(0).getValue(number, time) +
                getOperator(1).getValue(number, time) +
                getOperator(2).getValue(number, time) +
                getOperator(3).getValue(number, time)) / 4.0;
    }
}
