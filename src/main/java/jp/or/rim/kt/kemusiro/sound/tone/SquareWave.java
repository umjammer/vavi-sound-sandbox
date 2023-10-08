/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound.tone;

/**
 * 方形波を表すクラス。
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1 $
 */
public class SquareWave implements WaveGeneratable {

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

    /**
     * 指定の時刻の値を得る。波形の振幅は1.0に正規化される。
     *
     * @param number 音番号(0-127)
     * @param time   時刻
     * @return 波形値
     */
    @Override
    public double getValue(int number, double time) {
        double f = frequencyTable[number];

        if (normalize(f, time) < 0.5 / f) {
            return 1.0;
        } else {
            return -1.0;
        }
    }

    @Override
    public String toString() {
        return "Square Wave";
    }
}
