/*
 * Ported from Echo (https://github.com/IsaacMarovitz/Echo). Java port.
 */

package vavi.sound.xma;


/**
 * Codec-level description of an XMA stream.
 * <p>
 * An XMA file carries 1..N independent WMA Pro substreams, each with 1 or 2
 * channels. The total channel count is the sum of per-substream channels.
 * For mono/stereo content numStreams is 1; for 5.1 it is typically 3
 * (L/R, C/LFE, Ls/Rs); for 7.1 it is 4 (L/R, C/LFE, Ls/Rs, Lb/Rb).
 * <p>
 * Direct translation of {@code Echo/Container/XmaStreamInfo.cs}.
 */
public final class XmaStreamInfo {

    public XmaVersion version = XmaVersion.Xma2;
    public int sampleRate;
    public int channels;
    public int numStreams = 1;

    /**
     * Channels carried by a given substream (1 or 2). Returns 2 for stereo
     * streams in even-channel layouts, 1 otherwise.
     */
    public int channelsForStream(int streamIndex) {
        // Standard XMA layouts pair channels into stereo substreams. For odd
        // total channel counts the final substream is mono.
        int remaining = channels - 2 * streamIndex;
        return remaining >= 2 ? 2 : 1;
    }
}
