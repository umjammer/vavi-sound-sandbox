/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.rococoa;

import java.io.File;
import java.io.FileNotFoundException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFrame;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.rococoa.ObjCObjectByReference;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSArray;
import org.rococoa.cocoa.qtkit.QTKit;
import org.rococoa.cocoa.qtkit.QTMedia;
import org.rococoa.cocoa.qtkit.QTMovie;
import org.rococoa.cocoa.qtkit.QTMovieView;
import org.rococoa.cocoa.qtkit.QTTrack;

import vavi.sound.sampled.MonauralInputFilter;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * RococaFormatConversionProviderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060726 nsano initial version <br>
 * @see "https://github.com/iterate-ch/rococoa"
 */
@EnabledOnOs(OS.MAC)
public class RococaFormatConversionProviderTest {

    static final String inFile = "src/test/resources/test.caf";
    static final String outFile = "tmp/out.wav";

    static {
        @SuppressWarnings("unused")
        QTKit instance = QTKit.instance;
    }

    public static void main(String[] args) throws Exception {
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

    public static void test1(String[] args) throws Exception {
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

    /**
     * <ul>
     *  <li>→ mono に tritonus_remaining_###.jar が必要
     *  <li>eclipse では jar の順位がしたの方から plugin が機能している？？？
     * </ul>
     */
    @Test
    @Disabled
    public void test1() throws Exception {

for (Type type : AudioSystem.getAudioFileTypes()) {
    System.err.println("type: " + type);
}

        //
        int outSamplingRate = 8000;

        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new File(inFile));
Debug.println("IN: " + sourceAis.getFormat());

        AudioFormat inAudioFormat = sourceAis.getFormat();
        AudioFormat outAudioFormat = new AudioFormat(
            inAudioFormat.getEncoding(),
            outSamplingRate,
            inAudioFormat.getSampleSizeInBits(),
            inAudioFormat.getChannels(),
            inAudioFormat.getFrameSize(),
            inAudioFormat.getFrameRate(),
            inAudioFormat.isBigEndian());

        assertTrue(AudioSystem.isConversionSupported(outAudioFormat, inAudioFormat));

        //
        AudioInputStream secondAis = AudioSystem.getAudioInputStream(outAudioFormat, sourceAis);
Debug.println("OUT: " + secondAis.getFormat());
for (Type type : AudioSystem.getAudioFileTypes(secondAis)) {
 Debug.println("type: " + type);
}
Debug.println("secondAis: " + secondAis.getFormat());
        AudioInputStream thirdAis = new MonauralInputFilter().doFilter(secondAis);
Debug.println("thirdAis: " + thirdAis.getFormat());
        AudioSystem.write(thirdAis, AudioFileFormat.Type.WAVE, new File(outFile));

        // play
        AudioInputStream resultAis = AudioSystem.getAudioInputStream(new File(outFile));
        assertEquals(outSamplingRate, (int) resultAis.getFormat().getSampleRate());

        //
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, resultAis.getFormat());
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(resultAis.getFormat());
        line.addLineListener(new LineListener() {
            public void update(LineEvent ev) {
Debug.println(ev.getType());
                if (LineEvent.Type.STOP == ev.getType()) {
                    System.exit(0);
                }
            }
        });
        line.start();

FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
double gain = .1d; // number between 0 and 1 (loudest)
float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
gainControl.setValue(dB);

        byte[] buf = new byte[1024];
        while (true) {
            int r = resultAis.read(buf, 0, 1024);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }
        line.drain();
        line.stop();
        line.close();
    }

    @Test
    @Disabled("not completed yet")
    public void test2() throws Exception {
        for (AudioFileFormat.Type type : AudioSystem.getAudioFileTypes()) {
            System.err.println(type);
        }
        AudioInputStream originalAudioInputStream = AudioSystem.getAudioInputStream(new File(inFile).toURI().toURL());
        AudioFormat originalAudioFormat = originalAudioInputStream.getFormat();
Debug.println(originalAudioFormat);
    }
}

/* */
