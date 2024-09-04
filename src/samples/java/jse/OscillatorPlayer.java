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

package jse;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import gnu.getopt.Getopt;

import static java.lang.System.getLogger;


/*        +DocBookXML
        <title>Playing waveforms</title>

        <formalpara><title>Purpose</title>
        <para>
        Plays waveforms (sine, square, ...).
        </para></formalpara>

        <formalpara><title>Level</title>
        <para>Advanced</para></formalpara>

        <formalpara><title>Usage</title>
        <para>
        <cmdsynopsis>
        <command>java OscillatorPlayer</command>
        <arg><option>-t <replaceable>waveformtype</replaceable></option></arg>
        <arg><option>-f <replaceable>signalfrequency</replaceable></option></arg>
        <arg><option>-r <replaceable>samplerate</replaceable></option></arg>
        <arg><option>-a <replaceable>amplitude</replaceable></option></arg>
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
        </variablelist>
        </formalpara>

        <formalpara><title>Bugs, limitations</title>
        <para>
        Full-scale waves can lead to clipping. It is currently not known
        which component is responsible for this.
        </para></formalpara>

        <formalpara><title>Source code</title>
        <para>
        <ulink url="OscillatorPlayer.java.html">OscillatorPlayer.java</ulink>,
        <ulink url="Oscillator.java.html">Oscillator.java</ulink>,
        <ulink url="http://www.urbanophile.com/arenn/hacking/download.html">gnu.getopt.Getopt</ulink>
        </para></formalpara>

-DocBookXML
*/

/**
 * OscillatorPlayer.java
 * <p>
 * This file is part of the Java Sound Examples.
 */
public class OscillatorPlayer {

    private static final Logger logger = getLogger(OscillatorPlayer.class.getName());

    private static final int BUFFER_SIZE = 128000;

    public static void main(String[] args) throws IOException {
        byte[] abData;
        AudioFormat audioFormat;
        int nWaveformType = Oscillator.WAVEFORM_SINE;
        float fSampleRate = 44100.0F;
        float fSignalFrequency = 1000.0F;
        float fAmplitude = 0.7F;

        // Parsing of command-line options takes place...
        Getopt g = new Getopt("AudioPlayer", args, "ht:r:f:a:D");
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
                case '?':
                    printUsageAndExit();
                default:
                    logger.log(Level.DEBUG, "getopt() returned " + c);
                    break;
            }
        }

        audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                fSampleRate, 16, 2, 4, fSampleRate, false);

        AudioInputStream oscillator = new Oscillator(nWaveformType,
                fSignalFrequency, fAmplitude, audioFormat, AudioSystem.NOT_SPECIFIED);
        SourceDataLine line = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
        line.start();

        abData = new byte[BUFFER_SIZE];
        while (true) {
            logger.log(Level.DEBUG, "OscillatorPlayer.main(): trying to read (bytes): " + abData.length);

            int nRead = oscillator.read(abData);
            logger.log(Level.DEBUG, "OscillatorPlayer.main(): in loop, read (bytes): " + nRead);

            int nWritten = line.write(abData, 0, nRead);
            logger.log(Level.DEBUG, "OscillatorPlayer.main(): written: " + nWritten);
        }
    }

    private static int getWaveformType(String strWaveformType) {
        int nWaveformType = Oscillator.WAVEFORM_SINE;
        strWaveformType = strWaveformType.trim().toLowerCase();
        nWaveformType = switch (strWaveformType) {
            case "sine" -> Oscillator.WAVEFORM_SINE;
            case "square" -> Oscillator.WAVEFORM_SQUARE;
            case "triangle" -> Oscillator.WAVEFORM_TRIANGLE;
            case "sawtooth" -> Oscillator.WAVEFORM_SAWTOOTH;
            default -> nWaveformType;
        };
        return nWaveformType;
    }

    private static void printUsageAndExit() {
        logger.log(Level.DEBUG, "OscillatorPlayer: usage:");
        logger.log(Level.DEBUG, "\tjava OscillatorPlayer [-t <waveformtype>] [-f <signalfrequency>] [-r <samplerate>]");
        System.exit(1);
    }
}
