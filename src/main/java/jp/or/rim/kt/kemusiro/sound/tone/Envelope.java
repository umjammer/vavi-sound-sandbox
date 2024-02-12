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
public abstract class Envelope {
    protected double time = 0;
    protected double step = 0;
    private boolean released = false;

    protected Envelope() {
    }

    public void setTimeStep(double step) {
        this.step = step;
    }

    public void press() {
        released = false;
        time = 0;
    }

    public void release() {
        released = true;
        time = 0;
    }

    protected abstract double getValueInPressing();

    protected abstract double getValueInReleasing();

    public double getValue() {
        double value;
        if (released) {
            value = getValueInReleasing();
        } else {
            value = getValueInPressing();
        }
        time += step;
        return value;
    }
}
