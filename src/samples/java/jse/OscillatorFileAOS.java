/*
 * Copyright (c) 1999 -2001 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
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

import gnu.getopt.Getopt;
import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.tritonus.share.sampled.AudioSystemShadow;
import org.tritonus.share.sampled.file.AudioOutputStream;
import org.tritonus.share.sampled.file.TDataOutputStream;

import static java.lang.System.getLogger;


/*
 * +DocBookXML <title>Saving waveform data to a file (<classname>AudioOutputStream</classname>
 * version)</title>
 *
 * <formalpara><title>Purpose</title> <para>Generates waveform data (sine,
 * square, ...) and saves them to a file. This program uses Tritonus'
 * <classname>AudioOutputStream</classname> architecture to write the file.</para>
 * </formalpara>
 *
 * <formalpara><title>Level</title> <para>experienced</para></formalpara>
 *
 * <formalpara><title>Usage</title> <para> <cmdsynopsis> <command>java
 * OscillatorFileAOS</command> <arg><option>-t <replaceable>waveformtype</replaceable></option></arg>
 * <arg><option>-f <replaceable>signalfrequency</replaceable></option></arg>
 * <arg><option>-r <replaceable>samplerate</replaceable></option></arg>
 * <arg><option>-a <replaceable>amplitude</replaceable></option></arg> <arg
 * choice="plain"><replaceable>audiofile</replaceable></arg> </cmdsynopsis>
 * </para> </formalpara>
 *
 * <formalpara><title>Parameters</title> <variablelist> <varlistentry> <term><option>-t
 * <replaceable>waveformtype</replaceable></option></term> <listitem><para>the
 * waveform to play. One of sine, sqaure, triangle and sawtooth. Default: sine.</para></listitem>
 * </varlistentry> <varlistentry> <term><option>-f <replaceable>signalfrequency</replaceable></option></term>
 * <listitem><para>the frequency of the signal to create. Default: 1000 Hz.</para></listitem>
 * </varlistentry> <varlistentry> <term><option>-r <replaceable>samplerate</replaceable></option></term>
 * <listitem><para>the sample rate to use. Default: 44.1 kHz.</para></listitem>
 * </varlistentry> <varlistentry> <term><option>-a <replaceable>amplitude</replaceable></option></term>
 * <listitem><para>the amplitude of the generated signal. May range from 0.0 to
 * 1.0. 1.0 means a full-scale wave. Default: 0.7.</para></listitem>
 * </varlistentry> <varlistentry> <term><replaceable>audiofile</replaceable></term>
 * <listitem><para>the name of the audio file to store the resulting waveform
 * in.</para></listitem> </varlistentry> </variablelist> </formalpara>
 *
 * <formalpara><title>Bugs, limitations</title> <para> Using
 * <classname>AudioOutputStream</classname>s from Tritons makes this program
 * not portable. Full-scale waves can lead to clipping. It currently not known
 * which component is responsible for this. </para></formalpara>
 *
 * <formalpara><title>Source code</title> <para> <ulink
 * url="OscillatorFileAOS.java.html">OscillatorFileAOS.java</ulink>, <ulink
 * url="Oscillator.java.html">Oscillator.java</ulink>, <ulink
 * url="http://www.urbanophile.com/arenn/hacking/download.html">gnu.getopt.Getopt</ulink>,
 * <ulink url="http://www.tritonus.org/">Tritonus</ulink> </para> </formalpara>
 *
 * -DocBookXML
 */

/**
 * OscillatorFileAOS
 */
public class OscillatorFileAOS {

    private static final Logger logger = getLogger(OscillatorFileAOS.class.getName());

    private static final int BUFFER_SIZE = 128000;

    public static void main(String[] args) throws IOException {
        AudioFormat audioFormat;
        int nWaveformType = Oscillator.WAVEFORM_SINE;
        float fSampleRate = 44100.0F;
        float fSignalFrequency = 1000.0F;
        float fAmplitude = 0.7F;
        AudioFileFormat.Type targetType = AudioFileFormat.Type.AU;

        // The desired duration of the file in seconds. This can be set by the
        // '-d' command line switch. Default is 10 seconds.
        int nDuration = 10;

        // Parsing of command-line options takes place...
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
            case '?':
                printUsageAndExit();
            default:
                logger.log(Level.DEBUG, "getopt() returned " + c);
                break;
            }
        }

        /*
         * We make shure that there is only one more argument, which we take as
         * the filename of the soundfile to store to.
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

        audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, fSampleRate, 16, 2, 4, fSampleRate, false);

        int nLengthInFrames = Math.round(nDuration * fSampleRate);
        AudioInputStream oscillator = new Oscillator(nWaveformType, fSignalFrequency, fAmplitude, audioFormat, nLengthInFrames);

        TDataOutputStream dataOutputStream = null;
        try {
//            dataOutputStream = new TSeekableDataOutputStream(targetFile);
            dataOutputStream = AudioSystemShadow.getDataOutputStream(outputFile);
        } catch (IOException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }

        AudioOutputStream audioOutputStream = null;

        // AudioFileFormat.Type type = audioFileFormat.getType();
        long lLengthInFrames = oscillator.getFrameLength();

        // TODO: move to AudioSystemShadow.getAudioOutputStream()
        long lLengthInBytes = AudioSystem.NOT_SPECIFIED;
        if (lLengthInFrames != AudioSystem.NOT_SPECIFIED) {
            lLengthInBytes = lLengthInFrames * audioFormat.getFrameSize();
        }

        audioOutputStream = AudioSystemShadow.getAudioOutputStream(targetType, audioFormat, lLengthInBytes, dataOutputStream);

        /*
         * Ok, finally the line is prepared. Now comes the real job: we have to
         * write data to the line. We do this in a loop. First, we read data
         * from the AudioInputStream to a buffer. Then, we write from this
         * buffer to the Line. This is done until the end of the file is
         * reached, which is detected by a return value of -1 from the read
         * method of the AudioInputStream.
         */
        int nBytesRead = 0;
        byte[] abData = new byte[BUFFER_SIZE];
        while (nBytesRead != -1) {
            logger.log(Level.DEBUG, "OscillatorFileAOS.main(): trying to read (bytes): " + abData.length);
            nBytesRead = oscillator.read(abData, 0, abData.length);
            logger.log(Level.DEBUG, "OscillatorFileAOS.main(): read (bytes): " + nBytesRead);
            if (nBytesRead >= 0) {
                int nBytesWritten = audioOutputStream.write(abData, 0, nBytesRead);
                logger.log(Level.DEBUG, "OscillatorFileAOS.main(): written: " + nBytesWritten);
            }
        }

        /*
         * All data are transfered. We can close the shop. Note that this is
         * important to do backpatching of the header, if possible.
         */
        audioOutputStream.close();
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
        System.out.println("OscillatorFileAOS: usage:");
        System.out.println("\tjava OscillatorFileAOS [-t <waveformtype>] [-f <signalfrequency>] [-r <samplerate>] [-d <duration>] <filename>");
        System.exit(1);
    }
}
