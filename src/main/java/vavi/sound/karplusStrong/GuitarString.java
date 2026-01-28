/*
 * https://github.com/dspence84/GuitarHero
 */

package vavi.sound.karplusStrong;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import static java.lang.System.getLogger;


/**
 * Dependencies : RingBuffer
 * Description  :
 * this will create a simulated guitar string at the given frequency.
 * Included are functions to pluck() the string and tic() the simulation
 * (advance the simulation by one step).
 */
public class GuitarString {

    private static final Logger logger = getLogger(GuitarString.class.getName());

    /** the buffer that will hold the data for our simulation */
    private final RingBuffer stringBuffer;
    /** the length of our buffer */
    private int stringLength;
    /** the amount of time (in 'tics') the simulation has been running */
    private int time;
    /** Sampling rate to use for this simulation */
    private static final int SAMPLING_RATE = 44100;
    /** constant for they wave decay function */
    private static final double ENERGY_DECAY_FACTOR = .996;

    /** create a guitar string of the given frequency, using a sampling rate of 44,100 */
    public GuitarString(double frequency) {
        time = 0;
        int length = (int) Math.ceil(SAMPLING_RATE / frequency);
        stringLength = length;
        stringBuffer = new RingBuffer(length);
        // create a RingBuffer filled with 0s.
        while (!stringBuffer.isFull())
            stringBuffer.enqueue(0);
    }

    /** create a guitar string whose size and initial values are given by the array */
    public GuitarString(double[] init) {
        time = 0;
        stringBuffer = new RingBuffer(init.length);
        // set the buffer to initial values given in init
        for (double v : init) {
            stringBuffer.enqueue(v);
        }
    }

    /** pluck the guitar string by replacing the buffer with white noise */
    public void pluck() {
        // Pluck. Replace all items in the ring buffer with N random values between -0.5 and +0.5.
        for (int i = 0; i < stringLength; i++) {
            // find a random value in [0.5-0.5) and queue it
            double rnd = Math.random() - 0.5;
            stringBuffer.enqueue(rnd);
        }
    }

    /** advance the simulation one time step */
    public void tic() {
        // dequeue the buffer and peek it to find the average of the two values
        double first = stringBuffer.dequeue();
        double second = sample();
        // equation from the assignment website
        stringBuffer.enqueue(ENERGY_DECAY_FACTOR * (first + second) / 2);
        // increment the time
        time++;
    }

    /** return the current sample by peeking the buffer */
    public double sample() {
        return stringBuffer.peek();
    }

    /** return number of times tic was called so far */
    public int time() {
        return time;
    }

    /** test client, tests all methods */
    public static void main(String[] args) {
        int N = Integer.parseInt(args[0]);
        double[] samples = {.2, .4, .5, .3, -.2, .4, .3, .0, -.1, -.3};
        GuitarString testString = new GuitarString(samples);
        for (int i = 0; i < N; i++) {
            int t = testString.time();
            double sample = testString.sample();
            logger.log(Level.DEBUG, "%6d %8.4f".formatted(t, sample));
            testString.tic();
        }
    }
}
