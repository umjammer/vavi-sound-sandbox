package jse;
/*
 *        AudioConcat.java
 *
 *        This file is part of the Java Sound Examples.
 */
/*
 *  Copyright (c) 1999 - 2001 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
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
import gnu.getopt.Getopt;
import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import static java.lang.System.getLogger;


// TODO: the name AudioConcat is no longer appropriate. There should be a name that is neutral to concat/mix.

/*        +DocBookXML
        <title>Concatenating or mixing audio files</title>

        <formalpara><title>Purpose</title>
        <para>This program reads multiple audio files and
        writes a single one either
        containing the data of all the other
        files in order (concatenation mode, option <option>-c</option>)
        or containing a mixdown of all the other files
        (mixing mode, option <option>-m</option>).
        For concatenation, the input files must have the same audio
        format. They need not have the same file type.</para>
        </formalpara>

        <formalpara><title>Level</title>
        <para>experienced</para>
        </formalpara>

        <formalpara><title>Usage</title>
        <para>
        <cmdsynopsis>
        <command>java AudioConcat</command>
        <group choice="plain">
        <arg><option>-c</option></arg>
        <arg><option>-m</option></arg>
        </group>
        <arg choice="plain"><option>-o <replaceable>outputfile</replaceable></option></arg>
        <arg choice="plain" rep="repeat"><replaceable>inputfile</replaceable></arg>
        </cmdsynopsis>
        </para>
        </formalpara>

        <formalpara><title>Parameters</title>
        <variablelist>
        <varlistentry>
        <term><option>-c</option></term>
        <listitem><para>selects concatenation mode</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-m</option></term>
        <listitem><para>selects mixing mode</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-o <replaceable>outputfile</replaceable></option></term>
        <listitem><para>The filename of the output file</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><replaceable>inputfile</replaceable></term>
        <listitem><para>the name(s) of input file(s)</para></listitem>
        </varlistentry>
        </variablelist>
        </formalpara>

        <formalpara><title>Bugs, limitations</title>
        <para>
        This program is not well-tested. Output is always a WAV
        file. Future versions should be able to convert
        different audio formats to a dedicated target format.
        </para></formalpara>

        <formalpara><title>Source code</title>
        <para>
        <ulink url="AudioConcat.java.html">AudioConcat.java</ulink>,
        <ulink url="SequenceAudioInputStream.java.html">SequenceAudioInputStream.java</ulink>,
        <ulink url="MixingAudioInputStream.java.html">MixingAudioInputStream.java</ulink>,
        <ulink url="http://www.urbanophile.com/arenn/hacking/download.html">gnu.getopt.Getopt</ulink>
        </para>
        </formalpara>

-DocBookXML
*/
public class AudioConcat {

    private static final Logger logger = getLogger(AudioConcat.class.getName());

    private static final int MODE_NONE = 0;
    private static final int MODE_MIXING = 1;
    private static final int MODE_CONCATENATION = 2;

    public static void main(String[] args) {
        // Mode of operation.
        // Determines what to do with the input files:
        // either mixing or concatenation.
        int nMode = MODE_NONE;
        String strOutputFilename = null;
        AudioFormat audioFormat = null;
        List<AudioInputStream> audioInputStreamList = new ArrayList<>();

//      int nExternalBufferSize = DEFAULT_EXTERNAL_BUFFER_SIZE;
//      int nInternalBufferSize = AudioSystem.NOT_SPECIFIED;

        // Parsing of command-line options takes place...
        Getopt g = new Getopt("AudioConcat", args, "hcmo:");
        int c;
        while ((c = g.getopt()) != -1) {
            switch (c) {
            case 'h':
                printUsageAndExit();
            case 'o':
                strOutputFilename = g.getOptarg();
                logger.log(Level.DEBUG, "AudioConcat.main(): output filename: " + strOutputFilename);
                break;
            case 'c':
                nMode = MODE_CONCATENATION;
                break;
            case 'm':
                nMode = MODE_MIXING;
                break;
            case '?':
                printUsageAndExit();
            default:
                System.out.println("AudioConcat.main(): getopt() returned " + c);
                break;
            }
        }

        // All remaining arguments are assumed to be filenames of
        // soundfiles we want to play.
        String strFilename = null;
        for (int i = g.getOptind(); i < args.length; i++) {
            strFilename = args[i];

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

            AudioFormat format = audioInputStream.getFormat();

            // The first input file determines the audio format. This stream's
            // AudioFormat is stored. All other streams are checked against
            // this format.
            if (audioFormat == null) {
                audioFormat = format;
                logger.log(Level.DEBUG, "AudioConcat.main(): format: " + audioFormat);
            } else if (!audioFormat.matches(format)) {
                // TODO try to convert
                System.out.println("AudioConcat.main(): WARNING: AudioFormats don't match");
                System.out.println("AudioConcat.main(): master format: " + audioFormat);
                System.out.println("AudioConcat.main(): this format: " + format);
            }
            audioInputStreamList.add(audioInputStream);
        }

        if (audioFormat == null) {
            System.out.println("No input filenames!");
            printUsageAndExit();
        }

        AudioInputStream audioInputStream = null;
        switch (nMode) {
        case MODE_CONCATENATION:
            audioInputStream = new SequenceAudioInputStream(audioFormat, audioInputStreamList);
            break;
        case MODE_MIXING:
            audioInputStream = new MixingAudioInputStream(audioFormat, audioInputStreamList);
            break;
        default:
            System.out.println("you have to specify a mode (either -m or -c).");
            printUsageAndExit();
        }

        if (strOutputFilename == null) {
            System.out.println("you have to specify an output filename (using -o <filename>).");
            printUsageAndExit();
        }

        File outputFile = new File(strOutputFilename);
        try {
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
        } catch (IOException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }

        logger.log(Level.DEBUG, "AudioConcat.main(): before exit");
        System.exit(0);
    }

    private static void printUsageAndExit() {
        System.out.println("AudioConcat: usage:");
        System.out.println("\tjava AudioConcat -c|-m -o <outputfile> <inputfile> ...");
        System.exit(1);
    }
}
