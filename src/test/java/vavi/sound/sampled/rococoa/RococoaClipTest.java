/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.rococoa;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.Mixer;
import javax.swing.JFrame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.rococoa.ObjCObjectByReference;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSArray;
import org.rococoa.cocoa.qtkit.QTMedia;
import org.rococoa.cocoa.qtkit.QTMovie;
import org.rococoa.cocoa.qtkit.QTMovieView;
import org.rococoa.cocoa.qtkit.QTTrack;

import vavi.util.Debug;

import vavix.rococoa.avfoundation.AVAudioPlayer;

import static vavi.sound.SoundUtil.volume;


/**
 * RococoaClipTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/21 umjammer initial version <br>
 */
@EnabledOnOs(OS.MAC)
class RococoaClipTest {

    static double volume = Double.parseDouble(System.getProperty("vavi.test.volume",  "0.2"));

    static final String inFile = "/test.caf";

    public static void main(String[] args) throws Exception {
        URI uri = RococoaClipTest.class.getResource(inFile).toURI();
        AVAudioPlayer player = AVAudioPlayer.init(uri);
        if (player == null) {
            throw new FileNotFoundException(inFile);
        }
Debug.println("player: " + player);
        boolean r = player.prepareToPlay();
Debug.println("prepareToPlay: " + r);
        r = player.play();
Debug.println("play: " + r);
        player.setVolume((float) volume);
        long duration = (long) Math.ceil((float) player.duration() * 1000);
Debug.println("duration: " + duration);
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Deprecated
    static void test2(String[] args) throws Exception {
        ObjCObjectByReference ref = new ObjCObjectByReference();
        QTMovie movie = QTMovie.movieWithFile_error(inFile, ref);
        if (movie == null) {
            System.err.println(ref);
            throw new FileNotFoundException(inFile);
        }
        NSArray soundTracks = movie.tracksOfMediaType(QTMedia.QTMediaTypeSound);
        for (int i = 0; i < soundTracks.count(); i++) {
            QTTrack track = Rococoa.cast(soundTracks.objectAtIndex(i), QTTrack.class);
            track.setVolume(0.2f);
        }
        movie.play();
        long duration = (long) Math.ceil((float) movie.duration().timeValue / movie.duration().timeScale.shortValue() * 1000);
Debug.println("duration: " + duration + ", " + movie.duration().timeValue + ", " + movie.duration().timeScale + ", " + (movie.duration().timeValue / movie.duration().timeScale.shortValue()));
        Thread.sleep(duration);
    }

    @Deprecated
    static void test1(String[] args) throws Exception {
        QTMovieView movieView = QTMovieView.CLASS.create();
        movieView.setControllerVisible(true);
        movieView.setPreservesAspectRatio(true);
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        QTMovie movie = QTMovie.movieWithFile_error(inFile, null);
        movieView.setMovie(movie);
        movie.gotoBeginning();
        frame.pack();
        frame.setVisible(true);
    }

    @Test
    @DisabledIfSystemProperty(named = "os.arch", matches = "x86_64")
    void test1() throws Exception {
        Path path = Paths.get(RococoaClipTest.class.getResource(inFile).toURI());

        Mixer mixer = AudioSystem.getMixer(RococoaMixer.mixerInfo);
Debug.println(mixer.getMixerInfo());
        Clip clip = (Clip) mixer.getLine(mixer.getLineInfo());
Debug.println(clip.getLineInfo());
        CountDownLatch countDownLatch = new CountDownLatch(1);
        clip.addLineListener(event -> {
Debug.println("LINE: " + event.getType());
            if (event.getType().equals(LineEvent.Type.STOP)) {
                countDownLatch.countDown();
            }
        });
        AudioInputStream stream = new AudioInputStream(new BufferedInputStream(Files.newInputStream(path)), RococoaClip.info.getFormats()[0], Files.size(path));
        clip.open(stream);
try {
        volume(clip, volume);
} catch (Exception e) {
 Debug.println("volume: " + e);
}
Debug.println(clip.getFormat());
        clip.start();

if (!System.getProperty("vavi.test", "").equals("ide")) {
 Thread.sleep(10 * 1000);
 clip.stop();
 Debug.println("not on ide");
} else {
        countDownLatch.await();
}

        clip.close();
    }
}
