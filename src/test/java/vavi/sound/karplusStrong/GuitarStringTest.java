/*
 * https://github.com/dspence84/GuitarHero
 */

package vavi.sound.karplusStrong;

public class GuitarStringTest {

    public static void main(String[] args) {
        int N = Integer.parseInt(args[0]);
        double[] samples = {.2, .4, .5, .3, -.2, .4, .3, .0, -.1, -.3};
        GuitarString testString = new GuitarString(samples);
        for (int i = 0; i < N; i++) {
            int t = testString.time();
            double sample = testString.sample();
            System.out.printf("%6d %8.4f\n", t, sample);
            testString.tic();
        }
    }
}
