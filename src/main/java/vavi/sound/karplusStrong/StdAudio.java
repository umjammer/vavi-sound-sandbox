/*
 * https://github.com/dspence84/GuitarHero
 */

package vavi.sound.karplusStrong;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URL;

import static java.lang.System.getLogger;


/**
 * <i>Standard audio</i>. This class provides a basic capability for
 * creating, reading, and saving audio.
 * <p>
 * The audio format uses a sampling rate of 44,100 (CD quality audio), 16-bit, monaural.
 *
 * <p>
 * For additional documentation, see <a href="http://introcs.cs.princeton.edu/15inout">Section 1.5</a> of
 * <i>Introduction to Programming in Java: An Interdisciplinary Approach</i> by Robert Sedgewick and Kevin Wayne.
 * <p>
 * ---
 * <p>
 * Compilation:  javac StdAudio.java
 * Execution:    java StdAudio
 * <p>
 * Simple library for reading, writing, and manipulating .wav files.
 * <p>
 * <p>
 * Limitations
 * -----------
 * - Does not seem to work properly when reading .wav files from a .jar file.
 * - Assumes the audio is monaural, with sampling rate of 44,100.
 *
 * @author Robert Sedgewick
 * @author Kevin Wayne
 */
public final class StdAudio {

    private static final Logger logger = getLogger(StdAudio.class.getName());

    /**
     * The sample rate - 44,100 Hz for CD quality audio.
     */
    public static final int SAMPLE_RATE = 44100;

    /** 16-bit audio */
    private static final int BYTES_PER_SAMPLE = 2;
    /** 16-bit audio */
    private static final int BITS_PER_SAMPLE = 16;
    /** 32,767 */
    private static final double MAX_16_BIT = Short.MAX_VALUE;
    private static final int SAMPLE_BUFFER_SIZE = 4096;

    /** to play the sound */
    private static SourceDataLine line;
    /** our internal buffer */
    private static byte[] buffer;
    /** number of samples currently in internal buffer */
    private static int bufferSize = 0;

    /** do not instantiate */
    private StdAudio() {
    }

    /* static initializer */
    static {
        init();
    }

    /** open up an audio stream */
    private static void init() {
        try {
            // 44,100 samples per second, 16-bit audio, mono, signed PCM, little Endian
            AudioFormat format = new AudioFormat((float) SAMPLE_RATE, BITS_PER_SAMPLE, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format, SAMPLE_BUFFER_SIZE * BYTES_PER_SAMPLE);

            // the internal buffer is a fraction of the actual buffer size, this choice is arbitrary
            // it gets divided because we can't expect the buffered data to line up exactly with when
            // the sound card decides to push out its samples.
            buffer = new byte[SAMPLE_BUFFER_SIZE * BYTES_PER_SAMPLE / 3];
        } catch (Exception e) {
            logger.log(Level.DEBUG, e.getMessage());
            System.exit(1);
        }

        // no sound gets made before this call
        line.start();
    }

    /**
     * Close standard audio.
     */
    public static void close() {
        line.drain();
        line.stop();
    }

    /**
     * Write one sample (between -1.0 and +1.0) to standard audio. If the sample
     * is outside the range, it will be clipped.
     */
    public static void play(double in) {

        // clip if outside [-1, +1]
        if (in < -1.0) in = -1.0;
        if (in > +1.0) in = +1.0;

        // convert to bytes
        short s = (short) (MAX_16_BIT * in);
        buffer[bufferSize++] = (byte) s;
        buffer[bufferSize++] = (byte) (s >> 8);   // little Endian

        // send to sound card if buffer is full        
        if (bufferSize >= buffer.length) {
            line.write(buffer, 0, buffer.length);
            bufferSize = 0;
        }
    }

    /**
     * Write an array of samples (between -1.0 and +1.0) to standard audio. If a sample
     * is outside the range, it will be clipped.
     */
    public static void play(double[] input) {
        for (double v : input) {
            play(v);
        }
    }

    /**
     * Read audio samples from a file (in .wav or .au format) and return them as a double array
     * with values between -1.0 and +1.0.
     */
    public static double[] read(String filename) {
        byte[] data = readByte(filename);
        int N = data.length;
        double[] d = new double[N / 2];
        for (int i = 0; i < N / 2; i++) {
            d[i] = ((short) (((data[2 * i + 1] & 0xFF) << 8) + (data[2 * i] & 0xFF))) / ((double) MAX_16_BIT);
        }
        return d;
    }

    /**
     * Play a sound file (in .wav, .mid, or .au format) in a background thread.
     */
    public static void play(String filename) {
        try {
            URL url = null;
            File file = new File(filename);
            if (file.canRead()) url = file.toURI().toURL();

            // URL url = StdAudio.class.getResource(filename);
            if (url == null) throw new RuntimeException("audio " + filename + " not found");
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(url));
            clip.loop(1);
        } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    /**
     * Loop a sound file (in .wav, .mid, or .au format) in a background thread.
     */
    public static void loop(String filename) {
        try {
            URL url = null;
            File file = new File(filename);
            if (file.canRead()) url = file.toURI().toURL();

            // URL url = StdAudio.class.getResource(filename);
            if (url == null) throw new RuntimeException("audio " + filename + " not found");
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(url));
            clip.loop(1);
        } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    /** return data as a byte array */
    private static byte[] readByte(String filename) {
        byte[] data = null;
        AudioInputStream ais = null;
        try {

            // try to read from file
            File file = new File(filename);
            if (file.exists()) {
                ais = AudioSystem.getAudioInputStream(file);
                data = new byte[ais.available()];
                ais.read(data);
            }

            // try to read from URL
            else {
                URL url = StdAudio.class.getResource(filename);
                ais = AudioSystem.getAudioInputStream(url);
                data = new byte[ais.available()];
                ais.read(data);
            }
        } catch (Exception e) {
            logger.log(Level.DEBUG, e.getMessage());
            throw new RuntimeException("Could not read " + filename);
        }

        return data;
    }

    /**
     * Save the double array as a sound file (using .wav or .au format).
     */
    public static void save(String filename, double[] input) {

        // assumes 44,100 samples per second
        // use 16-bit audio, mono, signed PCM, little Endian
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        byte[] data = new byte[2 * input.length];
        for (int i = 0; i < input.length; i++) {
            int temp = (short) (input[i] * MAX_16_BIT);
            data[2 * i + 0] = (byte) temp;
            data[2 * i + 1] = (byte) (temp >> 8);
        }

        // now save the file
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            AudioInputStream ais = new AudioInputStream(bais, format, input.length);
            if (filename.endsWith(".wav") || filename.endsWith(".WAV")) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(filename));
            } else if (filename.endsWith(".au") || filename.endsWith(".AU")) {
                AudioSystem.write(ais, AudioFileFormat.Type.AU, new File(filename));
            } else {
                throw new RuntimeException("File format not supported: " + filename);
            }
        } catch (Exception e) {
            logger.log(Level.DEBUG, e);
            System.exit(1);
        }
    }

    //
    // sample test client
    //

    /**
     * create a note (sine wave) of the given frequency (Hz), for the given
     * duration (seconds) scaled to the given volume (amplitude)
     */
    private static double[] note(double hz, double duration, double amplitude) {
        int N = (int) (StdAudio.SAMPLE_RATE * duration);
        double[] a = new double[N + 1];
        for (int i = 0; i <= N; i++)
            a[i] = amplitude * Math.sin(2 * Math.PI * i * hz / StdAudio.SAMPLE_RATE);
        return a;
    }

    /**
     * Test client - play an A major scale to standard audio.
     */
    public static void main(String[] args) {

        // 440 Hz for 1 sec
        double freq = 440.0;
        for (int i = 0; i <= StdAudio.SAMPLE_RATE; i++) {
            StdAudio.play(0.5 * Math.sin(2 * Math.PI * freq * i / StdAudio.SAMPLE_RATE));
        }

        // scale increments
        int[] steps = {0, 2, 4, 5, 7, 9, 11, 12};
        for (int step : steps) {
            double hz = 440.0 * Math.pow(2, step / 12.0);
            StdAudio.play(note(hz, 1.0, 0.5));
        }

        // need to call this in non-interactive stuff so the program doesn't terminate
        // until all the sound leaves the speaker.
        StdAudio.close();

        // need to terminate a Java program with sound
        System.exit(0);
    }
}