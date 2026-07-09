/*
 * Ported from Echo (https://github.com/IsaacMarovitz/Echo). Java port.
 */

package vavi.sound.xma;


/**
 * Generates sine windows used by WMA Pro's IMDCT overlap-add. Windows are
 * cached per length; WMA Pro uses power-of-two lengths between 128 and 2048.
 * <p>
 * Direct translation of {@code Echo/WmaPro/SineWindow.cs}.
 *
 * @see "https://github.com/IsaacMarovitz/Echo"
 */
public final class SineWindow {

    private SineWindow() {}

    private static final float[][] cache = new float[12][];

    /** Returns a {@code sin((n + 0.5) * pi / (2N))} window of the given length. */
    public static synchronized float[] get(int length) {
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
            throw new IllegalArgumentException("SineWindow length must be a power of two.");
        }
        return Integer.numberOfTrailingZeros(value);
    }
}
