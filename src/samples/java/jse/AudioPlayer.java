package jse;
/*
 *  Copyright (c) 1999, 2000 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
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
 */

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import gnu.getopt.Getopt;
import org.apache.tools.ant.taskdefs.Java;

import static java.lang.System.getLogger;


/*        +DocBookXML
        <title>Playing an audio file (advanced)</title>

        <formalpara><title>Purpose</title>
        <para>
        Plays a single audio file. Capable of playing some
        compressed audio formats (A-law, &mu;-law, maybe mp3, GSM06.10).
        Allows control over buffering
        and which mixer to use.
        </para></formalpara>

        <formalpara><title>Level</title>
        <para>Advanced</para></formalpara>

        <formalpara><title>Usage</title>
        <para>
        <cmdsynopsis>
        <command>java AudioPlayer</command>
        <arg choice="plain"><option>-l</option></arg>
        </cmdsynopsis>
        <cmdsynopsis>
        <command>java AudioPlayer</command>
        <arg><option>-M <replaceable>mixername</replaceable></option></arg>
        <arg><option>-e <replaceable>buffersize</replaceable></option></arg>
        <arg><option>-i <replaceable>buffersize</replaceable></option></arg>
        <arg choice="plain"><replaceable>audiofile</replaceable></arg>
        </cmdsynopsis>
        </para></formalpara>

        <formalpara><title>Parameters</title>
        <variablelist>
        <varlistentry>
        <term><option>-l</option></term>
        <listitem><para>lists the available mixers</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-M <replaceable>mixername</replaceable></option></term>
        <listitem><para>selects a mixer to play on</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-e <replaceable>buffersize</replaceable></option></term>
        <listitem><para>the buffer size to use in the application ("extern")</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-i <replaceable>buffersize</replaceable></option></term>
        <listitem><para>the buffer size to use in Java Sound ("intern")</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option><replaceable>audiofile</replaceable></option></term>
        <listitem><para>the file name of the audio file to play</para></listitem>
        </varlistentry>
        </variablelist>
        </formalpara>

        <formalpara><title>Bugs, limitations</title>
        <para>
        Compressed formats can be handled depending on the
        capabilities of the Java Sound implementation.
        A-law and &mu;-law can be handled in any known Java Sound implementation.
        mp3 and GSM 06.10 can be handled by Tritonus. If you want to play these
        formats with the Sun jdk1.3/1.4, you have to install the respective plug-ins
        from <ulink url
        ="http://www.tritonus.org/plugins.html">Tritonus Plug-ins</ulink>.
        </para>
        </formalpara>

        <formalpara><title>Source code</title>
        <para>
        <ulink url="AudioPlayer.java.html">AudioPlayer.java</ulink>,
        <olink targetdocent="getopt">gnu.getopt.Getopt</olink>
        </para>
        </formalpara>

-DocBookXML
*/

/**
 * AudioPlayer.java
 * <p>
 * This file is part of the Java Sound Examples.
 */
public class AudioPlayer {

    private static final Logger logger = getLogger(AudioPlayer.class.getName());

    private static final int DEFAULT_EXTERNAL_BUFFER_SIZE = 128000;

    public static void main(String[] args) {
        String strMixerName = null;
        int nExternalBufferSize = DEFAULT_EXTERNAL_BUFFER_SIZE;
        int nInternalBufferSize = AudioSystem.NOT_SPECIFIED;

        // Parsing of command-line options takes place...
        Getopt g = new Getopt("AudioPlayer", args, "hlM:e:i:D");
        int c;
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'h':
                    printUsageAndExit();
                case 'l':
                    listMixersAndExit();
                case 'M':
                    strMixerName = g.getOptarg();
                    logger.log(Level.DEBUG, "AudioPlayer.main(): mixer name: " + strMixerName);
                    break;
                case 'e':
                    nExternalBufferSize = Integer.parseInt(g.getOptarg());
                    break;
                case 'i':
                    nInternalBufferSize = Integer.parseInt(g.getOptarg());
                    break;
                case '?':
                    printUsageAndExit();
                default:
                    System.out.println("getopt() returned " + c);
                    break;
            }
        }

        // We make shure that there is only one more argument, which
        // we take as the filename of the soundfile we want to play.
        String strFilename = null;
        for (int i = g.getOptind(); i < args.length; i++) {
            if (strFilename == null) {
                strFilename = args[i];
            } else {
                printUsageAndExit();
            }
        }
        if (strFilename == null) {
            printUsageAndExit();
        }

        File soundFile = new File(strFilename);

        // We have to read in the sound file.
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(soundFile);
        } catch (Exception e) {
            // In case of an exception, we dump the exception
            // including the stack trace to the console output.
            // Then, we exit the program.
            logger.log(Level.ERROR, e.getMessage(), e);
            System.exit(1);
        }

        // From the AudioInputStream, i.e. from the sound file,
        // we fetch information about the format of the
        // audio data.
        // These information include the sampling frequency,
        // the number of
        // channels and the size of the samples.
        // These information
        // are needed to ask Java Sound for a suitable output line
        // for this audio file.
        AudioFormat audioFormat = audioInputStream.getFormat();
        logger.log(Level.DEBUG, "AudioPlayer.main(): format: " + audioFormat);

        SourceDataLine line = getSourceDataLine(strMixerName, audioFormat, nInternalBufferSize);

        // second chance for compression formats.
        if (line == null) {
            AudioFormat sourceFormat = audioFormat;
            AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    sourceFormat.getSampleRate(),
                    16,
                    sourceFormat.getChannels(),
                    sourceFormat.getChannels() * 2,
                    sourceFormat.getSampleRate(),
                    false);
            logger.log(Level.DEBUG, "AudioPlayer.<init>(): source format: " + sourceFormat);
            logger.log(Level.DEBUG, "AudioPlayer.<init>(): target format: " + targetFormat);
            audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
            audioFormat = audioInputStream.getFormat();
            logger.log(Level.DEBUG, "AudioPlayer.<init>(): received target format: " + audioFormat);
            line = getSourceDataLine(strMixerName, audioFormat, nInternalBufferSize);
        }

        if (line == null) {
            System.out.println("AudioPlayer: cannot get SourceDataLine for format " + audioFormat);
            System.exit(1);
        }

        // Still not enough. The line now can receive data,
        // but will not pass them on to the audio output device
        // (which means to your sound card). This has to be
        // activated.
        line.start();

        // Ok, finally the line is prepared. Now comes the real
        // job: we have to write data to the line. We do this
        // in a loop. First, we read data from the
        // AudioInputStream to a buffer. Then, we write from
        // this buffer to the Line. This is done until the end
        // of the file is reached, which is detected by a
        // return value of -1 from the read method of the
        // AudioInputStream.
        int nBytesRead = 0;
        byte[] abData = new byte[nExternalBufferSize];
        while (nBytesRead != -1) {
            try {
                nBytesRead = audioInputStream.read(abData, 0, abData.length);
            } catch (IOException e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
            logger.log(Level.DEBUG, "AudioPlayer.main(): read from AudioInputStream (bytes): " + nBytesRead);
            if (nBytesRead >= 0) {
                int nBytesWritten = line.write(abData, 0, nBytesRead);
                logger.log(Level.DEBUG, "AudioPlayer.main(): written to SourceDataLine (bytes): " + nBytesWritten);
            }
        }

        // Wait until all data is played.
        // This is only necessary because of the bug noted below.
        // (If we do not wait, we would interrupt the playback by
        // prematurely closing the line and exiting the VM.)
        //
        // Thanks to Margie Fitch for bringing me on the right
        // path to this solution.
        logger.log(Level.DEBUG, "AudioPlayer.main(): before drain");
        line.drain();

        // All data are played. We can close the shop.
        logger.log(Level.DEBUG, "AudioPlayer.main(): before close");
        line.close();

        // There is a bug in the Sun jdk1.3.
        // It prevents correct termination of the VM.
        // So we have to exit ourselves.
        logger.log(Level.DEBUG, "AudioPlayer.main(): before exit");
        System.exit(0);
    }

    private static void printUsageAndExit() {
        System.out.println("AudioPlayer: usage:");
        System.out.println("\tjava AudioPlayer -l");
        System.out.println("\tjava AudioPlayer [-M <mixername>] <soundfile>");
        System.exit(1);
    }

    private static void listMixersAndExit() {
        System.out.println("Available Mixers:");

        Mixer.Info[] aInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info aInfo : aInfos) {
            System.out.println(aInfo.getName());
        }
        if (aInfos.length == 0) {
            System.out.println("[No mixers available]");
        }
        System.exit(0);
    }

    /**
     * This method tries to return a Mixer.Info whose name
     * matches the passed name. If no matching Mixer.Info is
     * found, null is returned.
     */
    private static Mixer.Info getMixerInfo(String strMixerName) {
        Mixer.Info[] aInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info aInfo : aInfos) {
            if (aInfo.getName().equals(strMixerName)) {
                return aInfo;
            }
        }
        return null;
    }

    private static SourceDataLine getSourceDataLine(String strMixerName, AudioFormat audioFormat, int nBufferSize) {
        // Asking for a line is a rather tricky thing.
        // We have to construct an Info object that specifies
        // the desired properties for the line.
        // First, we have to say which kind of line we want. The
        // possibilities are: SourceDataLine (for playback), Clip
        // (for repeated playback)        and TargetDataLine (for
        //  recording).
        // Here, we want to do normal playback, so we ask for
        // a SourceDataLine.
        // Then, we have to pass an AudioFormat object, so that
        // the Line knows which format the data passed to it
        // will have.
        // Furthermore, we can give Java Sound a hint about how
        // big the internal buffer for the line should be. This
        // isn't used here, signaling that we
        // don't care about the exact size. Java Sound will use
        // some default value for the buffer size.
        SourceDataLine line = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, nBufferSize);
        try {
            if (strMixerName != null) {
                Mixer.Info mixerInfo = getMixerInfo(strMixerName);
                if (mixerInfo == null) {
                    System.out.println("AudioPlayer: mixer not found: " + strMixerName);
                    System.exit(1);
                }

                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                line = (SourceDataLine) mixer.getLine(info);
            } else {
                line = (SourceDataLine) AudioSystem.getLine(info);
            }

            // The line is there, but it is not yet ready to
            // receive audio data. We have to open the line.
            line.open(audioFormat, nBufferSize);
        } catch (Exception e) {
            logger.log(Level.DEBUG, e.getMessage(), e);
        }
        return line;
    }
}
