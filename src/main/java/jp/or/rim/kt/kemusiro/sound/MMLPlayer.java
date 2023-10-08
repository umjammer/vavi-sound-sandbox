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

    private static void usage() {
        System.out.println("java MMLPlayer MML1 [MML2 [MML3]]");
        System.exit(1);
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

    /**
     * 引数で与えられたMML文字列を演奏する。
     *
     * @param args [-f instr.txt] mml1.mml [mm2.mml [mm3.mml]]
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
        }
        if (args[0].equals("-f")) {
            FMGeneralInstrument.readParameter(new FileReader(args[1]));
            String[] new_args = new String[args.length - 2];
            for (int i = 2; i < args.length; i++) {
                new_args[i - 2] = args[i];
            }
            args = new_args;
        } else {
            FMGeneralInstrument.readParameterByResource();
        }
        CountDownLatch cdl = new CountDownLatch(1);
        MMLPlayer p = new MMLPlayer(e -> {
            if (e.getType() == LineEvent.Type.STOP) cdl.countDown();
        });
        String[] mmls = new String[args.length];
        int i = 0;
        for (String arg : args) {
            mmls[i++] = String.join("", Files.readAllLines(Paths.get(arg)));
        }
        p.setMML(mmls);
        p.start();
        cdl.await();
Debug.println("here");
        p.stop();
Thread.getAllStackTraces().keySet().forEach(System.err::println);
    }
}
