/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound.tone;

/**
 * An interface that can generate audio.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1 $
 */
public interface WaveGeneratable {

    /**
     * Get the value at the specified time. The amplitude of the waveform is normalized to 1.0.
     *
     * @param number note number (0-127)
     * @param time   time
     * @return waveform value
     */
    double getValue(int number, double time);
}
