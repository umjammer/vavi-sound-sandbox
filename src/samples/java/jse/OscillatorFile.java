package jse;
/*
 *        OscillatorFile.java
 *
 *        This file is part of the Java Sound Examples.
 */
/*
 *  Copyright (c) 1999 -2001 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
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
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;


/*        +DocBookXML
        <title>Saving waveform data to a file (standard version)</title>

        <formalpara><title>Purpose</title>
        <para>Generates waveform data (sine, square, ...) and saves them
        to a file.
        This program uses <function>AudioSystem.write()</function>
        to write the file.</para>
        </formalpara>

        <formalpara><title>Level</title>
        <para>Advanced</para></formalpara>

        <formalpara><title>Usage</title>
        <para>
        <cmdsynopsis>
        <command>java OscillatorFile</command>
        <arg><option>-t <replaceable>waveformtype</replaceable></option></arg>
        <arg><option>-f <replaceable>signalfrequency</replaceable></option></arg>
        <arg><option>-r <replaceable>samplerate</replaceable></option></arg>
        <arg><option>-a <replaceable>amplitude</replaceable></option></arg>
        <arg choice="plain"><replaceable>audiofile</replaceable></arg>
        </cmdsynopsis>
        </para>
        </formalpara>

        <formalpara><title>Parameters</title>
        <variablelist>
        <varlistentry>
        <term><option>-t <replaceable>waveformtype</replaceable></option></term>
        <listitem><para>the waveform to play. One of sine, sqaure, triangle and sawtooth. Default: sine.</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-f <replaceable>signalfrequency</replaceable></option></term>
        <listitem><para>the frequency of the signal to create. Default: 1000 Hz.</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-r <replaceable>samplerate</replaceable></option></term>
        <listitem><para>the sample rate to use. Default: 44.1 kHz.</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><option>-a <replaceable>amplitude</replaceable></option></term>
        <listitem><para>the amplitude of the generated signal. May range from 0.0 to 1.0. 1.0 means a full-scale wave. Default: 0.7.</para></listitem>
        </varlistentry>
        <varlistentry>
        <term><replaceable>audiofile</replaceable></term>
        <listitem><para>the name of the audio file to store the resulting
        waveform in.</para></listitem>
        </varlistentry>
        </variablelist>
        </formalpara>

        <formalpara><title>Bugs, limitations</title>
        <para>
        Full-scale waves can lead to clipping. It currently not known
        which component is responsible for this.
        </para></formalpara>

        <formalpara><title>Source code</title>
        <para>
        <ulink url="OscillatorFile.java.html">OscillatorFile.java</ulink>,
        <ulink url="Oscillator.java.html">Oscillator.java</ulink>,
        <ulink url="http://www.urbanophile.com/arenn/hacking/download.html">gnu.getopt.Getopt</ulink>
        </para>
        </formalpara>

-DocBookXML
*/
public class OscillatorFile {
//  private static final int BUFFER_SIZE = 128000;
    private static boolean DEBUG = false;

    public static void main(String[] args) throws IOException {
//      byte[] abData;
        AudioFormat audioFormat;
        int nWaveformType = Oscillator.WAVEFORM_SINE;
        float fSampleRate = 44100.0F;
        float fSignalFrequency = 1000.0F;
        float fAmplitude = 0.7F;
        AudioFileFormat.Type targetType = AudioFileFormat.Type.AU;

        /**        The desired duration of the file in seconds.
                This can be set by the '-d' command line switch.
                Default is 10 seconds.
        */
        int nDuration = 10;

        /*
         *        Parsing of command-line options takes place...
         */
        Getopt g = new Getopt("AudioPlayer", args, "ht:r:f:a:d:D");
        int c;
        while ((c = g.getopt()) != -1) {
            switch (c) {
            case 'h':
                printUsageAndExit();
            case 't':
                nWaveformType = getWaveformType(g.getOptarg());
                break;
            case 'r':
                fSampleRate = Float.parseFloat(g.getOptarg());
                break;
            case 'f':
                fSignalFrequency = Float.parseFloat(g.getOptarg());
                break;
            case 'a':
                fAmplitude = Float.parseFloat(g.getOptarg());
                break;
            case 'd':
                nDuration = Integer.parseInt(g.getOptarg());
                break;
            case 'D':
                DEBUG = true;
                break;
            case '?':
                printUsageAndExit();
            default:
                if (DEBUG) {
                    out("getopt() returned " + c);
                }
                break;
            }
        }

        /*
          We make shure that there is only one more argument,
          which we take as the filename of the soundfile to store to.
         */
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

        File outputFile = new File(strFilename);

        audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                      fSampleRate, 16, 2, 4, fSampleRate, false);

        int nLengthInFrames = Math.round(nDuration * fSampleRate);
        AudioInputStream oscillator = new Oscillator(nWaveformType,
                                                     fSignalFrequency,
                                                     fAmplitude, audioFormat,
                                                     nLengthInFrames);

        AudioSystem.write(oscillator, targetType, outputFile);
    }

    private static int getWaveformType(String strWaveformType) {
        int nWaveformType = Oscillator.WAVEFORM_SINE;
        strWaveformType = strWaveformType.trim().toLowerCase();
        if (strWaveformType.equals("sine")) {
            nWaveformType = Oscillator.WAVEFORM_SINE;
        } else if (strWaveformType.equals("square")) {
            nWaveformType = Oscillator.WAVEFORM_SQUARE;
        } else if (strWaveformType.equals("triangle")) {
            nWaveformType = Oscillator.WAVEFORM_TRIANGLE;
        } else if (strWaveformType.equals("sawtooth")) {
            nWaveformType = Oscillator.WAVEFORM_SAWTOOTH;
        }
        return nWaveformType;
    }

    private static void printUsageAndExit() {
        out("OscillatorFile: usage:");
        out("\tjava OscillatorFile [-t <waveformtype>] [-f <signalfrequency>] [-r <samplerate>] [-d <duration>] <filename>");
        System.exit(1);
    }

    private static void out(String strMessage) {
        System.out.println(strMessage);
    }
}
