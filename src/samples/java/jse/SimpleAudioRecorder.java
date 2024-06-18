/*
 *  Copyright (c) 1999 - 2001 by Matthias Pfisterer <Matthias.Pfisterer@web.de>
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

package jse;

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import static java.lang.System.getLogger;


/*        +DocBookXML
        <title>Recording to an audio file (simple version)</title>

        <formalpara><title>Purpose</title>
        <para>Records audio data and stores it in a file. The data is
        recorded in CD quality (44.1 kHz, 16 bit linear, stereo) and
        stored in a <filename>.wav</filename> file.</para></formalpara>

        <formalpara><title>Level</title>
        <para>newbie</para></formalpara>

        <formalpara><title>Usage</title>
        <para>
        <cmdsynopsis>
        <command>java SimpleAudioRecorder</command>
        <arg choice="plain"><replaceable>audiofile</replaceable></arg>
        </cmdsynopsis>
        </para></formalpara>

        <formalpara><title>Parameters</title>
        <variablelist>
        <varlistentry>
        <term><option><replaceable>audiofile</replaceable></option></term>
        <listitem><para>the file name of the
        audio file that should be produced from the recorded data</para></listitem>
        </varlistentry>
        </variablelist>
        </formalpara>

        <formalpara><title>Bugs, limitations</title>
        <para>
        You cannot select audio formats and the audio file type
        on the command line. See
        AudioRecorder for a version that has more advanced options.
        Due to a bug in the Sun jdk1.3, this program does not work
        with it.
        </para></formalpara>

        <formalpara><title>Source code</title>
        <para>
        <ulink url="SimpleAudioRecorder.java.html">SimpleAudioRecorder.java</ulink>
        </para>
        </formalpara>

-DocBookXML
*/

/**
 * SimpleAudioRecorder.java
 * <p>
 * This file is part of the Java Sound Examples.
 */
public class SimpleAudioRecorder extends Thread {

    private static final Logger logger = getLogger(SimpleAudioRecorder.class.getName());

    private final TargetDataLine m_line;
    private final AudioFileFormat.Type m_targetType;
    private final AudioInputStream m_audioInputStream;
    private final File m_outputFile;
    private boolean m_bRecording;

    public SimpleAudioRecorder(TargetDataLine line, AudioFileFormat.Type targetType, File file) {
        m_line = line;
        m_audioInputStream = new AudioInputStream(line);
        m_targetType = targetType;
        m_outputFile = file;
    }

    /**
     * Starts the recording.
     * To accomplish this, (i) the line is started and (ii) the
     * thread is started.
     */
    @Override
    public void start() {
        m_line.start();
        super.start();
    }

    public void stopRecording() {
        m_line.stop();
        m_line.close();
        m_bRecording = false;
    }

    @Override
    public void run() {
        try {
            AudioSystem.write(m_audioInputStream, m_targetType, m_outputFile);
            System.out.println("after write()");
        } catch (IOException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsageAndExit();
        }

        // We make shure that there is only one more argument, which
        // we take as the filename of the soundfile to store to.
        String strFilename = args[0];
        File outputFile = new File(strFilename);

        AudioFormat audioFormat = null;

        // 8 kHz, 8 bit, mono
        // audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000.0F, 8, 1, 1, 8000.0F, true);
        // 44.1 kHz, 16 bit, stereo
        audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                44100.0F, 16, 2, 4, 44100.0F, false);

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
        TargetDataLine targetDataLine = null;
        try {
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(audioFormat);
        } catch (LineUnavailableException e) {
            System.out.println("unable to get a recording line");
            logger.log(Level.ERROR, e.getMessage(), e);
            System.exit(1);
        }

        AudioFileFormat.Type targetType = AudioFileFormat.Type.AU;
        SimpleAudioRecorder recorder = null;
        recorder = new SimpleAudioRecorder(targetDataLine, targetType, outputFile);
        System.out.println("Press ENTER to start the recording.");
        try {
//            System.in.read();
            System.in.read();
        } catch (IOException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
        recorder.start();
        System.out.println("Recording...");
        System.out.println("Press ENTER to stop the recording.");
        try {
            System.in.read();
        } catch (IOException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
        recorder.stopRecording();
        System.out.println("Recording stopped.");

//        System.exit(0);
    }

    private static void printUsageAndExit() {
        System.out.println("SimpleAudioRecorder: usage:");
        System.out.println("\tjava SimpleAudioRecorder <soundfile>");
        System.exit(0);
    }
}
