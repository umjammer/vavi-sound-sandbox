/*
 * WMA v1/v2 decoder. Sine windows (FFmpeg ff_sine_window_init).
 */

package vavi.sound.wma;


/**
 * Sine windows for the IMDCT overlap-add, cached per length. Mirrors FFmpeg's
 * {@code ff_sine_window_init}: {@code window[i] = sin((i + 0.5) * pi / (2N))}.
 */
final class SineWindow {

    private SineWindow() {}

    private static final float[][] cache = new float[16][];

    /** Returns the sine window of the given power-of-two length. */
    static synchronized float[] get(int length) {
        int log2 = log2Exact(length);
        float[] cached = cache[log2];
        if (cached != null) {
            return cached;
        }
        float[] window = new float[length];
        double scale = Math.PI / (2.0 * length);
        for (int i = 0; i < length; i++) {
            window[i] = (float) Math.sin((i + 0.5) * scale);
        }
        cache[log2] = window;
        return window;
    }

    private static int log2Exact(int value) {
        if (value <= 0 || (value & (value - 1)) != 0) {
            throw new IllegalArgumentException("window length must be a power of two.");
        }
        return Integer.numberOfTrailingZeros(value);
    }
}
