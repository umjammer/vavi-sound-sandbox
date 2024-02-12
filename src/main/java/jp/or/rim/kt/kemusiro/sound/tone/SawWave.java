/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound.tone;

/**
 * のこぎり波を発生する。
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1 $
 */
public class SawWave implements WaveGeneratable {

    private static final double[] frequencyTable;

    // normal temperament
    static {
        double base = 440.0;    // A: #69
        frequencyTable = new double[128];

        for (int i = 0; i < 128; i++) {
            frequencyTable[i] = base * Math.pow(2.0, ((double) (i - 69)) / 12.0);
        }
    }

    private static double normalize(double frequency, double time) {
        return time - Math.floor(time * frequency) / frequency;
    }

    @Override
    public double getValue(int number, double time) {
        double f = frequencyTable[number];
        return normalize(f, time) * 2 * f - 1.0;
    }
}
