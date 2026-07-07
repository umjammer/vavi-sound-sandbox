/*
 * Ported from Echo (https://github.com/IsaacMarovitz/Echo). Java port.
 */

package vavi.sound.xma;


/**
 * One decoded XMA frame's bitstream slice.
 * <p>
 * Direct translation of the {@code XmaFrame} struct in
 * {@code Echo/Container/XmaFrameReader.cs}.
 *
 * @param buffer self-contained buffer holding the frame's bits
 * @param startBit bit offset of the frame within {@code buffer}
 * @param lengthBits frame length in bits
 */
public record XmaFrame(byte[] buffer, int startBit, int lengthBits) {
}
