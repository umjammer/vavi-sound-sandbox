/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.File;
import java.util.concurrent.CountDownLatch;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;

import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.util.Debug;

import static vavi.sound.SoundUtil.volume;


/**
 * clip.
 * <p>
 * read all input stream then play
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/06/11 umjammer initial version <br>
 */
public class Test1 {

    static {
        System.setProperty("vavi.sound.opl3.MidiFile", "true"); // true: means using opl3 midi device when SMF format 0
    }

    static final String inFile = System.getProperty("user.home") + "/Music/midi/1/title-screen.mid";
//    static final String inFile = System.getProperty("user.home") + "/Music/midi/1/Rydeen.mid";
//    static final String inFile = System.getProperty("user.home") + "/Music/midi/1/ac4br_gm.MID";
//    static final String inFile = System.getProperty("user.home") + "/Music/midi/1/thexder.mid";
//    static final String inFile = "tmp/opl3/demo.cmf";
//    static final String inFile = "tmp/opl3/dro_v2.dro";
//    static final String inFile = "tmp/opl3/samurai.dro";
//    static final String inFile = "tmp/opl3/dune1.dro";
//    static final String inFile = "tmp/opl3/dott_dott_logo.laa";
//    static final String inFile = "tmp/opl3/michaeld.cmf";
//    static final String inFile = "/Users/nsano/Music/0/11 - Blockade.flac";
//    static final String inFile = "/Users/nsano/Music/0/11 - Blockade.m4a"; // ALAC
//    static final String inFile = "tmp/female_scrub.spx";
//    static final String inFile = "tmp/hoshiF.opus";

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        for (AudioFileFormat.Type type : AudioSystem.getAudioFileTypes()) {
            System.err.println(type);
        }
//        URL clipURL = new URL(args[0]);
//        AudioInputStream originalAudioInputStream = AudioSystem.getAudioInputStream(clipURL);
//        AudioInputStream originalAudioInputStream = AudioSystem.getAudioInputStream(new File(inFile).toURI().toURL());
        AudioInputStream originalAudioInputStream = AudioSystem.getAudioInputStream(new File(inFile));
        AudioFormat originalAudioFormat = originalAudioInputStream.getFormat();
Debug.println(originalAudioFormat);
        AudioFormat targetAudioFormat = new AudioFormat(
            originalAudioFormat.getSampleRate(),
            16,
            originalAudioFormat.getChannels(),
            true,
            false);
Debug.println(targetAudioFormat);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(targetAudioFormat, originalAudioInputStream);
        AudioFormat audioFormat = audioInputStream.getFormat();
        DataLine.Info info = new DataLine.Info(Clip.class, audioFormat, AudioSystem.NOT_SPECIFIED);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Clip clip = (Clip) AudioSystem.getLine(info);
Debug.println(clip.getClass().getName());
        clip.addLineListener(event -> {
            if (event.getType().equals(LineEvent.Type.START)) {
System.err.println("play");
            }
            if (event.getType().equals(LineEvent.Type.STOP)) {
System.err.println("done");
                countDownLatch.countDown();
            }
        });
        clip.open(audioInputStream);
if (!(originalAudioFormat.getEncoding() instanceof Opl3Encoding)) {
// Debug.println("down volume: " + originalAudioFormat.getEncoding());
 volume(clip, .2d);
}
        clip.start();
        countDownLatch.await();
        clip.close();
    }
}

/* */
