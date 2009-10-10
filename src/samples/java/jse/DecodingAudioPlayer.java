package jse;
/*
 *        DecodingAudioPlayer.java
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
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;


/*        +DocBookXML
        <title>Playing an encoded audio file</title>

        <formalpara><title>Purpose</title>
        <para>Plays an encoded audio file.</para>
        </formalpara>

        <formalpara><title>Level</title>
        <para>newbie</para></formalpara>

        <formalpara><title>Usage</title>
        <para>
        <cmdsynopsis><command>java DecodingAudioPlayer</command>
        <arg choice="plain"><replaceable class="parameter">encodedfile</replaceable></arg>
        </cmdsynopsis>
        </para>
        </formalpara>

        <formalpara><title>Parameters</title>
        <variablelist>
        <varlistentry>
        <term><replaceable class="parameter">encodedfile</replaceable></term>
        <listitem><para>the name of the encoded audio file to play</para></listitem>
        </varlistentry>
        </variablelist>
        </formalpara>

        <formalpara><title>Bugs, limitations</title>
        <para>Compressed formats can be handled depending on the
        capabilities of the Java Sound implementation it is run with.
        A-law and &mu;-law can be handled in any known Java Sound
        implementation. mp3 and GSM 06.10 can be handled by
        <ulink url="http://www.tritonus.org/">Tritonus</ulink>.
        If you want to play these formats with the Sun jdk1.3.0,
        you have to install the respective plug-ins from
        <ulink url="http://www.tritonus.org/plugins.html">Tritonus
        Plug-ins</ulink>. Depending on the Java Sound implementation,
        this program may or may not play unencoded files.
        For a safe way to handle encoded as well as unencoded files,
        see AudioPlayer.</para>
        </formalpara>

        <formalpara><title>Source code</title>
        <para>
        <ulink url="DecodingAudioPlayer.java.html">DecodingAudioPlayer.java</ulink>
        </para>
        </formalpara>

-DocBookXML
*/
public class DecodingAudioPlayer {
    private static final int EXTERNAL_BUFFER_SIZE = 128000;

    public static void main(String[] args) {
        if (args.length != 1) {
            printUsageAndExit();
        }

        String strFilename = args[0];
        byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(new File(strFilename));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (audioInputStream == null) {
            System.out.println("###  cannot read input file: " + strFilename);
        }

        AudioFormat sourceFormat = audioInputStream.getFormat();

/*
                AudioFormat        targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        sourceFormat.getSampleRate(),
                        16,
                        sourceFormat.getChannels(),
                        sourceFormat.getChannels() * 2,
                        sourceFormat.getSampleRate(),
                        false);
*/
        AudioFormat.Encoding targetEncoding = AudioFormat.Encoding.PCM_SIGNED;

        // audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
        audioInputStream = AudioSystem.getAudioInputStream(targetEncoding,
                                                           audioInputStream);

        AudioFormat audioFormat = audioInputStream.getFormat();

        SourceDataLine line = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        line.start();

        int nBytesRead = 0;
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

        // line.close();
    }

    public static void printUsageAndExit() {
        System.out.println("DecodingAudioPlayer: usage:");
        System.out.println("\tjava DecodingAudioPlayer <soundfile>");
        System.exit(1);
    }
}

/* */
