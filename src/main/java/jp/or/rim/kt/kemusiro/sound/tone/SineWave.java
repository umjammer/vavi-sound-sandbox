/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound.tone;

/**
 * sin波を表すクラス。
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
     * 指定の時刻の値を得る。波形の振幅は1.0に正規化される。
     *
     * @param number 音番号(0-127)
     * @param time   時刻
     * @return 波形値
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
