/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound.tone;

/**
 * Save various parameters of FM sound source.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1 $
 */
public class FMParameter {

    private final int toneNumber;
    private int algorithm;
    private final double[] multiplier;
    private final double[] attackRate;
    private final double[] decayRate;
    private final double[] sustainRate;
    private final double[] releaseRate;
    private final double[] sustainLevel;
    private final double[] maxLevel;

    public FMParameter(int number, int n) {
        multiplier = new double[n];
        attackRate = new double[n];
        decayRate = new double[n];
        sustainRate = new double[n];
        releaseRate = new double[n];
        sustainLevel = new double[n];
        maxLevel = new double[n];
        toneNumber = number;
    }

    public int getToneNumber() {
        return toneNumber;
    }

    public void setAlgorithm(int a) {
        algorithm = a;
    }

    public int getAlgorithm() {
        return algorithm;
    }

    public void setMultiplier(int op, double value) {
        multiplier[op] = value;
    }

    public double getMultiplier(int op) {
        return multiplier[op];
    }

    public void setAttackRate(int op, double value) {
        attackRate[op] = value;
    }

    public double getAttackRate(int op) {
        return attackRate[op];
    }

    public void setDecayRate(int op, double value) {
        decayRate[op] = value;
    }

    public double getDecayRate(int op) {
        return decayRate[op];
    }

    public void setSustainRate(int op, double value) {
        sustainRate[op] = value;
    }

    public double getSustainRate(int op) {
        return sustainRate[op];
    }

    public void setReleaseRate(int op, double value) {
        releaseRate[op] = value;
    }

    public double getReleaseRate(int op) {
        return releaseRate[op];
    }

    public void setSustainLevel(int op, double value) {
        sustainLevel[op] = value;
    }

    public double getSustainLevel(int op) {
        return sustainLevel[op];
    }

    public void setMaxLevel(int op, double value) {
        maxLevel[op] = value;
    }

    public double getMaxLevel(int op) {
        return maxLevel[op];
    }
}
