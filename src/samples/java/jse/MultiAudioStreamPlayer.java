
package jse;

/*
 *        MultiAudioStreamPlayer.java
 *
 *        This file is part of the Java Sound Examples.
 */
/*
 *  Copyright (c) 1999 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
 *
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;


// import AudioStream;

/*
 * +DocBookXML <title>Playing multiple audio files concurrently</title>
 *
 * <formalpara><title>Purpose</title> <para>This program plays multiple audio
 * files concurrently. It opens each file given on the command line and starts
 * it. The program uses the class <classname>AudioStream</classname>.</para>
 * </formalpara>
 *
 * <formalpara><title>Level</title> <para>experienced</para></formalpara>
 *
 * <formalpara><title>Usage</title> <para> <cmdsynopsis> <command>java
 * MultiAudioStreamPlayer</command> <arg choice="plain" rep="repeat"><replaceable>audiofile</replaceable></arg>
 * </cmdsynopsis> </para></formalpara>
 *
 * <formalpara><title>Parameters</title> <variablelist> <varlistentry> <term><replaceable>audiofile</replaceable></term>
 * <listitem><para>the name(s) of the audio file(s) to play</para></listitem>
 * </varlistentry> </variablelist> </formalpara>
 *
 * <formalpara><title>Bugs, limitations</title> <para>Not well-tested</para></formalpara>
 *
 * <formalpara><title>Source code</title> <para> <ulink
 * url="MultiAudioStreamPlayer.java.html">MultiAudioStreamPlayer.java</ulink>,
 * <ulink url="SimpleAudioStream.java.html">SimpleAudioStream.java</ulink>,
 * <ulink url="BaseAudioStream.java.html">BaseAudioStream.java</ulink> </para>
 * </formalpara>
 *
 * -DocBookXML
 */
public class MultiAudioStreamPlayer {
    public static void main(String[] args) {
        /*
         * We check that there is at least one command-line argument. If not, we
         * display the usage message and exit.
         */
        if (args.length < 1) {
            System.out.println("MultiAudioStreamPlayer: usage:");
            System.out.println("\tjava MultiAudioStreamPlayer <soundfile1> <soundfile2> ...");
            System.exit(1);
        }

        /*
         * Now, that we're shure there is at least one argument, we take each
         * argument as the filename of the soundfile we want to play.
         */
        for (int i = 0; i < args.length; i++) {
            String strFilename = args[i];
            File soundFile = new File(strFilename);

            /*
             * We just create an AudioStream object by passing a File object for
             * the soundfile to the constructor. All hairy details are handled
             * inside of AudioStream.
             */
            SimpleAudioStream audioStream = null;
            try {
                audioStream = new SimpleAudioStream(soundFile);
            } catch (LineUnavailableException e) {
                /*
                 * In case of an exception, we dump the exception including the
                 * stack trace to the console output. Then, we exit the program.
                 */
                e.printStackTrace();
                System.exit(1);
            } catch (UnsupportedAudioFileException e) {
                e.printStackTrace();
                System.exit(1);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                System.exit(1);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }

            /*
             * We start the playback.
             */
            audioStream.start();
        }

        /*
         * TODO: use some (yet to be defined) function in AudioStream to wait
         * until it is finished, then exit the VM.
         */
    }
}
