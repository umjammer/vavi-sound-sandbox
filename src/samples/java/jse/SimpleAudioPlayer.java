package jse;
/*
 *        SimpleAudioPlayer.java
 *
 *        This file is part of the Java Sound Examples.
 */
/*
 *  Copyright (c) 1999 - 2001 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
 *
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;


/*        +DocBookXML
        <title>Playing an audio file (easy)</title>

        <formalpara><title>Purpose</title>
        <para>Plays a single audio file.</para></formalpara>

        <formalpara><title>Level</title>
        <para>Newbies</para></formalpara>

        <formalpara><title>Usage</title>
        <cmdsynopsis>
        <command>java SimpleAudioPlayer</command>
        <replaceable class="parameter">audiofile</replaceable>
        </cmdsynopsis>
        </formalpara>

        <formalpara><title>Parameters</title>
        <variablelist>
        <varlistentry>
        <term><option><replaceable class="parameter">audiofile</replaceable></option></term>
        <listitem><para>the name of the
        audio file that should be played</para></listitem>
        </varlistentry>
        </variablelist>
        </formalpara>

        <formalpara><title>Bugs, limitations</title>
        <para>Only PCM encoded files are supported. A-law, &mu;-law,
        ADPCM, mp3 and other compressed data formats are not supported. For
        playing these, see AudioPlayer.</para>
        </formalpara>

        <formalpara><title>Source code</title>
        <para>
        <ulink url="SimpleAudioPlayer.java.html">SimpleAudioPlayer.java</ulink>
        </para>
        </formalpara>

-DocBookXML
*/
public class SimpleAudioPlayer {
    private static final int EXTERNAL_BUFFER_SIZE = 128000;

    public static void main(String[] args) {
        /*
          We check that there is exactely one command-line
          argument.
          If not, we display the usage message and exit.
        */
        if (args.length != 1) {
            System.out.println("SimpleAudioPlayer: usage:");
            System.out.println("\tjava SimpleAudioPlayer <soundfile>");
            System.exit(1);
        }

        /*
          Now, that we're shure there is an argument, we
          take it as the filename of the soundfile
          we want to play.
        */
        String strFilename = args[0];
        File soundFile = new File(strFilename);

        /*
          We have to read in the sound file.
        */
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(soundFile);
        } catch (Exception e) {
            /*
              In case of an exception, we dump the exception
              including the stack trace to the console output.
              Then, we exit the program.
            */
            e.printStackTrace();
            System.exit(1);
        }

        /*
          From the AudioInputStream, i.e. from the sound file,
          we fetch information about the format of the
          audio data.
          These information include the sampling frequency,
          the number of
          channels and the size of the samples.
          These information
          are needed to ask Java Sound for a suitable output line
          for this audio file.
        */
        AudioFormat audioFormat = audioInputStream.getFormat();

        /*
          Asking for a line is a rather tricky thing.
          We have to construct an Info object that specifies
          the desired properties for the line.
          First, we have to say which kind of line we want. The
          possibilities are: SourceDataLine (for playback), Clip
          (for repeated playback)        and TargetDataLine (for
          recording).
          Here, we want to do normal playback, so we ask for
          a SourceDataLine.
          Then, we have to pass an AudioFormat object, so that
          the Line knows which format the data passed to it
          will have.
          Furthermore, we can give Java Sound a hint about how
          big the internal buffer for the line should be. This
          isn't used here, signaling that we
          don't care about the exact size. Java Sound will use
          some default value for the buffer size.
        */
        SourceDataLine line = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);

            /*
              The line is there, but it is not yet ready to
              receive audio data. We have to open the line.
            */
            line.open(audioFormat);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        /*
          Still not enough. The line now can receive data,
          but will not pass them on to the audio output device
          (which means to your sound card). This has to be
          activated.
        */
        line.start();

        /*
          Ok, finally the line is prepared. Now comes the real
          job: we have to write data to the line. We do this
          in a loop. First, we read data from the
          AudioInputStream to a buffer. Then, we write from
          this buffer to the Line. This is done until the end
          of the file is reached, which is detected by a
          return value of -1 from the read method of the
          AudioInputStream.
        */
        int nBytesRead = 0;
        byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];
        while (nBytesRead != -1) {
            try {
                nBytesRead = audioInputStream.read(abData, 0, abData.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (nBytesRead >= 0) {
                int nBytesWritten = line.write(abData, 0, nBytesRead);
            }
        }

        /*
          Wait until all data are played.
          This is only necessary because of the bug noted below.
          (If we do not wait, we would interrupt the playback by
          prematurely closing the line and exiting the VM.)

          Thanks to Margie Fitch for bringing me on the right
          path to this solution.
        */
        line.drain();

        /*
          All data are played. We can close the shop.
        */
        line.close();

        /*
          There is a bug in the jdk1.3.
          It prevents correct termination of the VM.
          So we have to exit ourselves.
        */
        System.exit(0);
    }
}

/* */
