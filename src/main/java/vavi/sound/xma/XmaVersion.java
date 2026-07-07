/*
 * Ported from Echo (https://github.com/IsaacMarovitz/Echo). Java port.
 */

package vavi.sound.xma;


/**
 * XMA codec version.
 * <p>
 * Direct translation of the {@code XmaVersion} enum in
 * {@code Echo/Container/XmaStreamInfo.cs}.
 */
public enum XmaVersion {

    Xma1(1),
    Xma2(2);

    private final int value;

    XmaVersion(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
