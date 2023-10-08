/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import java.io.IOException;
import javax.sound.sampled.LineListener;

import vavi.util.Debug;


/**
 * MMLを演奏する。
 *
 * TODO glitch at the end
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision: 1.7 $
 */
public class MMLPlayer implements Runnable {

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
     * MML文字列を解析して再生する。
     * 演奏状態を監視し、演奏終了時にlockオブジェクトに対して
     * notify()が発行される。
     *
     * @param mmls MML文字列
     * @throws MMLException 不正なMML文字列か否か
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

    public void run() {
        try {
            play(mmls);
        } catch (Exception e) {
            Debug.println(e);
        }
    }

    public void setMML(String[] mmls) throws IOException {
        this.mmls = mmls;
    }
}
