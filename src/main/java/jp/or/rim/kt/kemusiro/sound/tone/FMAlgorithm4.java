/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound.tone;

/**
 * FM音源のアルゴリズム4。
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1 $
 */
public class FMAlgorithm4 extends FMAlgorithm {

    public FMAlgorithm4(FMParameter p) {
        super(4);

        SawWave pitch = new SawWave();
        setInput(0, pitch, null);
        setInput(1, pitch, getOperator(0));
        setInput(2, pitch, null);
        setInput(3, pitch, getOperator(2));
        setParameter(p);
    }

    public double getValue(int number, double time) {
        return 0.5 * (getOperator(1).getValue(number, time) +
                getOperator(3).getValue(number, time));
    }
}
