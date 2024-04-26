/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import vavi.util.Debug;


/**
 * Play streaming data.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.2 $
 */
public class StreamingSoundPlayer extends SoundPlayer {

    private SourceDataLine line = null;

    /**
     * Generate an object for audio playback.
     *
     * @param rate  sampling rate
     * @param depth sampling bit length
     */
    public StreamingSoundPlayer(int rate, int depth, LineListener listener) {
        format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                (float) rate, depth, 1, 1, (float) rate, true);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            if (listener != null) {
                line.addLineListener(listener);
            }
            line.open(format);
        } catch (LineUnavailableException e) {
            Debug.printStackTrace(e);
        }
    }

    /**
     * Returns line.
     *
     * @return line
     */
    @Override
    public DataLine getLine() {
        return line;
    }

    /**
     * Outputs data to a stream.
     *
     * @param buffer data to play
     */
    public void write(byte[] buffer, int offset, int length) {
        line.write(buffer, offset, length);
    }
}
