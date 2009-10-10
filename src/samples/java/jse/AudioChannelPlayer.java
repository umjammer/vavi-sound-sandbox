/*
 * Copyright (c) 2000 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
 *
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as published
 * by the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package jse;

import java.io.File;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;


/* OLD DOCUMENTATION:
        <td bgcolor="orange">
          <h2>AudioChannel -- playing sound files in a sequence</h2>

          <p>
            AudioChannel manages a queue of AudioInputStreams to send
            them to the same line.  In the constructor of
            AudioChannel, an AudioFormat has to be passed. A Line is
            retrieved for this format. AudioInputStreams having a
            different format are automatically converted during
            playback.  This class is in an experimental state. Please
            report problems.
          </p>
          <p>
            AudioChannelPlayer is a command-line program that shows
            how to use AudioChannel. It plays all sound files whose
            names are given as command line arguments in sequence.
          </p>
          <p>
            <strong>Source code:</strong> <a href="AudioChannel.java.html">AudioChannel.java</a>,
            <strong>Source code:</strong> <a href="AudioChannelPlayer.java.html">AudioChannelPlayer.java</a>
          </p>

        </td>
 */
/*
  +DocBookXML
        <title>Plays several soundfiles in sequence</title>
-DocBookXML
 */
/*
 *        AudioChannelPlayer.java
 *
 *        This file is part of the Java Sound Examples.
 */
public class AudioChannelPlayer {
    private static final boolean DEBUG = true;
    private static final int BUFFER_SIZE = 16384;

    public static void main(String[] args) {
        // TODO: set AudioFormat after the first soundfile
        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                                  44100.0F, 16, 2, 4, 44100.0F,
                                                  true);
        SourceDataLine line = null;

        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class,
                                                   audioFormat);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat, line.getBufferSize());
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        line.start();

        AudioChannel channel = new AudioChannel(line);
        channel.start();
        for (int nArgPos = 0; nArgPos < args.length; nArgPos++) {
            if (args[nArgPos].startsWith("-s")) {
                String strDuration = args[nArgPos].substring(2);
                int nDuration = Integer.parseInt(strDuration);
                handleSilence(nDuration, channel);
            } else {
                handleFile(args[nArgPos], channel);
            }
        }

        // TODO: instead of waiting a fixed amount of time, wait until the queue of AudioChannel is empty.
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
        }
    }

    private static void handleFile(String strFilename, AudioChannel channel) {
        File audioFile = new File(strFilename);
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(audioFile);
        } catch (Exception e) {
            /*
             *        In case of an exception, we dump the exception
             *        including the stack trace to the console output.
             *        Then, we exit the program.
             */
            e.printStackTrace();
            System.exit(1);
        }
        if (audioInputStream != null) {
            boolean bSuccessfull = channel.addAudioInputStream(audioInputStream);
            if (!bSuccessfull) {
                System.out.println("Warning: could not enqueue AudioInputStream; presumably formats don't match!");
            }
        }
    }

    private static void handleSilence(int nDuration, AudioChannel channel) {
    }
}

/* */
