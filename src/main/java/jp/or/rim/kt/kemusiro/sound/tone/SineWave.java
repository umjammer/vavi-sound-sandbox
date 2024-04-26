/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound.tone;

/**
 * A class representing a sine wave.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1 $
 */
public class SineWave implements WaveGeneratable {

    private static final double[] frequencyTable;

    // normal temperament
    static {
        double base = 440.0;    // A: #69
        frequencyTable = new double[128];

        for (int i = 0; i < 128; i++) {
            frequencyTable[i] = base * Math.pow(2.0, ((double) (i - 69)) / 12.0);
        }
    }

    public SineWave() {
    }

    /**
     * Get the value at the specified time. The amplitude of the waveform is normalized to 1.0.
     *
     * @param number note number (0-127)
     * @param time   time
     * @return waveform value
     */
    @Override
    public double getValue(int number, double time) {
        return Math.sin(2.0 * Math.PI * frequencyTable[number] * time);
    }

    @Override
    public String toString() {
        return "Sine Wave";
    }

}
