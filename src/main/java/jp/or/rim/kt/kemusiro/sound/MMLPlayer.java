/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.sampled.LineListener;

import static java.lang.System.getLogger;


/**
 * Plays MML.
 *
 * TODO glitch at the end
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.7 $
 */
public class MMLPlayer implements Runnable {

    private static final Logger logger = getLogger(MMLPlayer.class.getName());

    private static final int samplingRate = 22100;
    private static final int bitDepth = 8;

    private final LineListener listener;
    private Thread thread;
    private String[] mmls;

    private final StreamingSoundPlayer player;

    public MMLPlayer() {
        this(null);
    }

    public MMLPlayer(LineListener listener) {
        this.listener = listener;
        player = new StreamingSoundPlayer(samplingRate, bitDepth, listener);
        player.volume(0.02f);
    }

    public void setVolume(float gain) {
        player.volume(gain);
    }

    /**
     * Parse and play MML string.
     * The performance status is monitored, and notify() is issued
     * to the lock object when the performance ends.
     *
     * @param mmls MML string
     * @throws MMLException invalid MML string
     * @see SoundPlayer
     */
    public void play(String[] mmls) throws MMLException, IOException {
        int tickPerBeat = 240;
        MusicScore score = new MusicScore(tickPerBeat, mmls.length);
        MMLCompiler compiler = new MMLCompiler(tickPerBeat, mmls.length);
        WaveInputStream in = new WaveInputStream(score, samplingRate, bitDepth);

        compiler.compile(score, mmls);
        byte[] buffer = new byte[samplingRate * player.getFrameSize() / 2];
        player.start();
        int length = 0;
        while (thread != null && length >= 0) {
            length = in.read(buffer, 0, buffer.length);
            if (length >= 0) {
                player.write(buffer, 0, length);
            }
        }
        player.drain();
        player.stop();

        in.close();
    }

    public void start() {
        thread = new Thread(this);
        thread.setName("MMLPlayer");
        thread.start();
    }

    public void stop() throws InterruptedException {
        player.close();
        thread.join();
        thread = null;
    }

    @Override
    public void run() {
        try {
            play(mmls);
        } catch (Exception e) {
            logger.log(Level.INFO, e.getMessage(), e);
        }
    }

    public void setMML(String[] mmls) throws IOException {
        this.mmls = mmls;
    }
}
