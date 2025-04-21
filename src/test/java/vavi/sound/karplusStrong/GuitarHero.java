/*
 * https://github.com/dspence84/GuitarHero
 */

package vavi.sound.karplusStrong;


/**
 * Name(s)      : Daniel Spence
 * NetID(s)     : dspence
 * Precept(s)   : Section J
 * <p>
 * Dependencies : GuitarString, RingBuffer, StdDraw, StdAudio
 * Description  :
 * <p>
 * using the private member variable keyboard, will simulate a plucked guitar
 * string.  The program will continue to take input from the user until the
 * user manually closes.  Interfaces with StdAudio to output the sample to
 * the users sound card.
 */
public class GuitarHero {

    // the keyboard of valid keys and their position
    private static String keyboard = "q2we4r5ty7u8i9op-[=zxdcfvgbnjmk,.;/' ";

    public static void main(String[] args) {
        // crate an array to hold each guitar string
        GuitarString[] guitarString = new GuitarString[keyboard.length()];

        // compute the frequency of each key on the keyboard
        for (int i = 0; i < keyboard.length(); i++) {
            // this equation was taken from the assignment page to determine the note frequency
            double f = 440.0 * Math.pow(2.0, (i - 24.0) / 12.0);
            guitarString[i] = new GuitarString(f);
        }

        // run the simulation until the user terminates the program
        while (true) {

            // check if the user has typed a key; if so, process it   
            if (StdDraw.hasNextKeyTyped()) {

                // wait for input
                char key = StdDraw.nextKeyTyped();

                // if the key pressed is on the keyboard, pluck the string
                if (keyboard.contains(String.valueOf(key)))
                    guitarString[keyboard.indexOf(key)].pluck();

            }

            // compute the superposition of samples and advance the strings by one step
            double sample = 0;
            for (int i = 0; i < keyboard.length(); i++) {
                sample += guitarString[i].sample();
                guitarString[i].tic();
            }

            // play the sample on standard audio
            StdAudio.play(sample);
        }
    }
}
