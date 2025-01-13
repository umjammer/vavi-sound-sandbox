/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;

import vavi.util.Debug;

import static java.lang.System.getLogger;


/**
 * Play with the already created byte array.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.1 $
 */
public class ClipSoundPlayer extends SoundPlayer {

    private static final Logger logger = getLogger(ClipSoundPlayer.class.getName());

    private Clip line = null;

    /**
     * Generate an object for audio playback.
     *
     * @param rate  sampling rate
     * @param depth sampling bit length
     * @param array array of audio data to play
     */
    public ClipSoundPlayer(int rate, int depth, byte[] array) {
        format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                (float) rate, depth, 1, 1, (float) rate, true);
        DataLine.Info info = new DataLine.Info(Clip.class, format);
        try {
            line = (Clip) AudioSystem.getLine(info);
            line.open(format, array, 0, array.length);
        } catch (LineUnavailableException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    /**
     * Return line.
     *
     * @return line
     */
    @Override
    public DataLine getLine() {
        return line;
    }
}
