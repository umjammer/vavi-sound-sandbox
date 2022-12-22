/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound.tone;

/**
 * FM音源のアルゴリズム。
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1 $
 */
public abstract class FMAlgorithm implements WaveGeneratable {

    private FMOperator[] operators;

    public FMAlgorithm(int operatorCount) {
        operators = new FMOperator[operatorCount];
        for (int i = 0; i < operatorCount; i++) {
            operators[i] = new FMOperator();
        }
    }

    public FMOperator getOperator(int op) {
        return operators[op];
    }

    public void setInput(int op, WaveGeneratable in1, WaveGeneratable in2) {
        operators[op].setInput(in1, in2);
    }

    public void setMultiplier(int op, double multiplier) {
        operators[op].setMultiplier(multiplier);
    }

    public void setMask(int op, boolean mask) {
        operators[op].setMask(mask);
    }

    public void setEnvelope(int op, Envelope envelope) {
        operators[op].setEnvelope(envelope);
    }

    public void setTimeStep(double newTimeStep) {
        for (FMOperator operator : operators) {
            operator.setTimeStep(newTimeStep);
        }
    }

    public void press() {
        for (FMOperator operator : operators) {
            operator.press();
        }
    }

    public void release() {
        for (FMOperator operator : operators) {
            operator.release();
        }
    }

    @Override
    public abstract double getValue(int number, double time);

    protected void setParameter(FMParameter p) {
        for (int op = 0; op < 4; op++) {
            setMultiplier(op, p.getMultiplier(op));
            setEnvelope(op, new ADSREnvelope(p.getAttackRate(op),
                    p.getDecayRate(op),
                    p.getSustainRate(op),
                    p.getReleaseRate(op),
                    p.getSustainLevel(op),
                    p.getMaxLevel(op)));
        }
    }
}
