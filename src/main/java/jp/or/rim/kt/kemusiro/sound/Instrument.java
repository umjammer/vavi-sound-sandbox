/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import jp.or.rim.kt.kemusiro.sound.tone.Envelope;
import jp.or.rim.kt.kemusiro.sound.tone.WaveGeneratable;


/**
 * A class representing musical instruments.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.2 $
 */
public abstract class Instrument {

    protected WaveGeneratable wave;
    protected Envelope envelope;
    private double currentTime;
    private double timeStep;

    public void setTimeStep(double newTimeStep) {
        timeStep = newTimeStep;
        envelope.setTimeStep(newTimeStep);
    }

    /**
     * Obtain the waveform value at the current time. The amplitude of the waveform is normalized to 1.0.
     * After obtaining the value, update the current time by the preset time range.
     *
     * @param number note number (0-127)
     * @return waveform value
     */
    public double getValue(int number) {
        double value = envelope.getValue() * wave.getValue(number, currentTime);
        currentTime += timeStep;
        return value;
    }

    public void press() {
        currentTime = 0;
        envelope.press();
    }

    public void release() {
        envelope.release();
    }

    /**
     * Get the name of the instrument.
     *
     * @return instrument name
     */
    public abstract String getName();
}
