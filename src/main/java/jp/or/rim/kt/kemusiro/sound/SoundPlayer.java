/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;

import vavi.sound.SoundUtil;

import static java.lang.System.getLogger;


/**
 * Abstract class that provides operations to play with data.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.2 $
 */
public abstract class SoundPlayer {

    private static final Logger logger = getLogger(SoundPlayer.class.getName());

    protected AudioFormat format;

    /**
     * Returns the sampling rate as a float.
     *
     * @return sampling rate
     */
    public float getSampleRate() {
        return format.getSampleRate();
    }

    /**
     * Returns the frame size.
     *
     * @return frame size
     */
    public int getFrameSize() {
        return format.getFrameSize();
    }

    /**
     * Returns line.
     *
     * @return line
     */
    public abstract DataLine getLine();

    /**
     * Starts playing.
     */
    public void start() {
        getLine().start();
    }

    /**
     * Flush out data accumulated in the buffer.
     */
    public void drain() {
        getLine().drain();
    }

    /**
     * Ends playback.
     */
    public void stop() {
        getLine().stop();
    }

    /**
     * Closes the line.
     */
    public void close() {
        getLine().close();
    }

    /** Changes volume */
    public void volume(float gain) {
logger.log(Level.DEBUG, "volume: " + gain + ", " + getClass()); // works, but not so different
        SoundUtil.volume(getLine(), gain);
    }
}
