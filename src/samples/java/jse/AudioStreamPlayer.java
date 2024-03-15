package jse;
/*
 *        AudioStreamPlayer.java
 *
 *        This file is part of the Java Sound Examples.
 */
/*
 *  Copyright (c) 1999, 2000 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
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


/*        +DocBookXML
        <title>AudioStream - encapsulating audio file playback</title>

        <formalpara><title>Purpose</title>
        <para>AudioStream hides the details of loading an audio file, requesting a Line and feeding the data to the line. It includes support for changing gain and pan. AudioStreamPlayer is a command-line application that shows how to use the basic features of AudioStream. This class is in an experimental state. Please report problems.</para>
        </formalpara>

        <formalpara><title>Level</title>
        <para>experienced</para></formalpara>

        <formalpara><title>Usage</title>
        <para>
        <cmdsynopsis>
        <command>java AudioStreamPlayer</command>
        <arg choice="plain"><replaceable>audiofile</replaceable></arg>
        </cmdsynopsis>
        </para></formalpara>

        <formalpara><title>Parameters</title>
        <variablelist>
        <varlistentry>
        <term><replaceable>audiofile</replaceable></term>
        <listitem><para>the name of the audio file to play</para></listitem>
        </varlistentry>
        </variablelist>
        </formalpara>

        <formalpara><title>Bugs, limitations</title>
        <para>Not well-tested</para></formalpara>

        <formalpara><title>Source code</title>
        <para>
        <ulink url="AudioStreamPlayer.java.html">AudioStreamPlayer.java</ulink>,
        <ulink url="SimpleAudioStream.java.html">SimpleAudioStream.java</ulink>,
        <ulink url="BaseAudioStream.java.html">BaseAudioStream.java</ulink>
        </para>
        </formalpara>

-DocBookXML
*/
public class AudioStreamPlayer {
    public static void main(String[] args) {
        /*
         *        We check that there is exactely one command-line
         *        argument. If not, we display the usage message and
         *        exit.
         */
        if (args.length != 1) {
            printUsageAndExit();
        }

        /*
         *        Now, that we're shure there is an argument, we take
         *        it as the filename of the soundfile we want to play.
         */
        String strFilename = args[0];
        File soundFile = new File(strFilename);

        /*
         *        We just create a SimpleAudioStream by passing a
         *        File object for the soundfile to the constructor.
         *        All hairy details are handled inside of this class.
         */
        SimpleAudioStream audioStream = null;
        try {
            audioStream = new SimpleAudioStream(soundFile);
        } catch (LineUnavailableException e) {
            /*
             *        In case of an exception, we dump the exception
             *        including the stack trace to the console
             *        output. Then, we exit the program.
             */
            e.printStackTrace();
            System.exit(1);
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.exit(1);
        }

        /*
         *        We start the playback.
         */
        audioStream.start();

        /*
         *        TODO: use some (yet to be defined) function in
         *        SimpleAudioStream to wait until it is finished, then
         *        exit the VM.
         */
    }

    private static void printUsageAndExit() {
        System.out.println("AudioStreamPlayer: usage:");
        System.out.println("\tjava AudioStreamPlayer <soundfile>");
        System.exit(1);
    }
}
