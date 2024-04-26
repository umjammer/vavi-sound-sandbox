/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound.tone;

/**
 * A class representing an FM sound source operator.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1 $
 */
public class FMOperator implements WaveGeneratable {

    private WaveGeneratable pitch = null;
    private WaveGeneratable modulation = null;
    private Envelope envelope = null;
    private double multiplier = 1.0;
    private boolean mask = true;    // true if this operator is ON

    public FMOperator() {
    }

    public void setInput(WaveGeneratable in1, WaveGeneratable in2) {
        pitch = in1;
        modulation = in2;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public void setMask(boolean status) {
        this.mask = status;
    }

    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }

    public void setTimeStep(double newTimeStep) {
        envelope.setTimeStep(newTimeStep);
    }

    public void press() {
        envelope.press();
    }

    public void release() {
        envelope.release();
    }

    @Override
    public double getValue(int number, double time) {
        double in1;
        double in2 = 0.0;
        double result = 0.0;

        if (mask) {
            in1 = 2.0 * Math.PI * (pitch.getValue(number, time) + 1.0);
            if (modulation != null) {
                in2 = 2.0 * Math.PI * (modulation.getValue(number, time) + 1.0);
            }
            result = Math.sin(multiplier * (in1 + in2));
            if (envelope != null) {
                result *= envelope.getValue();
            }
        }
        return result;
    }
}
