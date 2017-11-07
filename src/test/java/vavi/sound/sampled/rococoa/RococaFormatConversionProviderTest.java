/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.rococoa;

import java.io.File;

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

import org.junit.Test;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSArray;
import org.rococoa.cocoa.qtkit.MovieComponent;
import org.rococoa.cocoa.qtkit.QTKit;
import org.rococoa.cocoa.qtkit.QTMedia;
import org.rococoa.cocoa.qtkit.QTMovie;
import org.rococoa.cocoa.qtkit.QTMovieView;
import org.rococoa.cocoa.qtkit.QTTrack;

import vavi.sound.sampled.MonauralInputFilter;
import vavi.util.Debug;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * RococaFormatConversionProviderTest.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060726 nsano initial version <br>
 * @see https://github.com/iterate-ch/rococoa
 */
public class RococaFormatConversionProviderTest {

//    static final String inFile = "/Users/nsano/Movies/790278e8.3gp";
//    static final String inFile = "/Users/nsano/Music/0/test001.m4a";
    static final String inFile = "/Users/nsano/Music/0/rc.aiff";
    static final String outFile = "out.wav";

    static {
       @SuppressWarnings("unused")
       QTKit instance = QTKit.instance;
    }

    public static void main(String[] args) throws Exception {
        QTMovie movie = QTMovie.movieWithFile_error(inFile, null);
        NSArray soundTracks = movie.tracksOfMediaType(QTMedia.QTMediaTypeSound);
        for (int i = 0; i < soundTracks.count(); i++) {
            QTTrack track = Rococoa.cast(soundTracks.objectAtIndex(i), QTTrack.class);
            track.setVolume(0.2f);
        }
        movie.play();
        long duration = movie.duration().timeValue * 1000;
System.err.println("duration: " + duration);
        Thread.sleep(duration);
    }

    public static void test1(String[] args) throws Exception {
        QTMovieView movieView = QTMovieView.CLASS.create();
        movieView.setControllerVisible(true);
        movieView.setPreservesAspectRatio(true);
        MovieComponent component = new MovieComponent(movieView);
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.getContentPane().add(component);
        QTMovie movie = QTMovie.movieWithFile_error(inFile, null);
        movieView.setMovie(movie);
        movie.gotoBeginning();
        frame.pack();
        frame.setVisible(true);
    }

    /**
     *
     * <ul>
     *  <li>→ mono に tritonus_remaining_###.jar が必要
     *  <li>eclipse では jar の順位がしたの方から plugin が機能している？？？
     * </ul>
     *
     */
    @Test
    public void test1() throws Exception {

for (Type type : AudioSystem.getAudioFileTypes()) {
    System.err.println("type: " + type);
}

        //
        int outSamplingRate = 8000;

        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new File(inFile));

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
System.err.println("OUT: " + secondAis.getFormat());
for (Type type : AudioSystem.getAudioFileTypes(secondAis)) {
    System.err.println("type: " + type);
}
System.err.println("secondAis: " + secondAis.getFormat());
        AudioInputStream thirdAis = new MonauralInputFilter().doFilter(secondAis);
System.err.println("thirdAis: " + thirdAis.getFormat());
        AudioSystem.write(thirdAis, AudioFileFormat.Type.WAVE, new File(outFile));

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
}

/* */
